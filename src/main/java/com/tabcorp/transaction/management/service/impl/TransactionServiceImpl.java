package com.tabcorp.transaction.management.service.impl;

import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import com.tabcorp.transaction.management.entity.Product;
import com.tabcorp.transaction.management.entity.Transaction;
import com.tabcorp.transaction.management.mapper.TransactionSummaryMapper;
import com.tabcorp.transaction.management.repository.TransactionRepository;
import com.tabcorp.transaction.management.repository.CustomerRepository;
import com.tabcorp.transaction.management.repository.ProductRepository;
import com.tabcorp.transaction.management.service.TransactionService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import jakarta.annotation.PostConstruct;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final MeterRegistry meterRegistry;
    private final TransactionSummaryMapper mapper;

    @Value("${transaction.processing.timeout:5}")
    private int processingTimeoutSeconds;

    @Value("${transaction.batch.size:100}")
    private int batchSize;

    @Value("${transaction.parallel.threads:3}")
    private int parallelThreads;

    private Timer processingTimer;

    @PostConstruct
    public void init() {
        processingTimer = Timer.builder("transaction.processing.time")
            .description("Time taken to process transactions")
            .register(meterRegistry);
    }

    @Override
    @CircuitBreaker(name = "processJsonTransactions")
    @Bulkhead(name = "processJsonTransactions")
    @CacheEvict(value = "transactionSummaryCache", allEntries = true)
    public Mono<List<Transaction>> processJsonTransactions(List<Transaction> transactions) {
        return processTransactionBatch(transactions, "JSON");
    }

    @Override
    @CircuitBreaker(name = "processBsonTransactions")
    @Bulkhead(name = "processBsonTransactions")
    @CacheEvict(value = "transactionSummaryCache", allEntries = true)
    public Mono<List<Transaction>> processBsonTransactions(List<Transaction> transactions) {
        return processTransactionBatch(transactions, "BSON");
    }

    public Mono<List<Transaction>> processTransactionBatch(List<Transaction> transactions, String format) {
        Timer.Sample timer = Timer.start();

        return Flux.fromIterable(transactions)
            .buffer(batchSize)  // Create sub-batches for optimal processing
            .parallel(parallelThreads)
            .runOn(Schedulers.boundedElastic())
            .flatMap(batch -> Flux.fromIterable(batch)
                .flatMap(transaction -> processTransaction(transaction)
                    .timeout(Duration.ofSeconds(processingTimeoutSeconds))
                    .doOnSuccess(t -> incrementSuccessMetric(format))
                    .doOnError(error -> handleProcessingError(error, format))
                    .onErrorResume(error -> handleTransactionError(error, transaction))
                )
            )
            .sequential()
            .collectList()
            .doFinally(signalType -> {
                timer.stop(processingTimer);
                log.info("Batch processing completed with signal: {}", signalType);
            });
    }

    @Override
    @CacheEvict(value = "transactionVolumeCache", allEntries = true)
    public Mono<Transaction> processTransaction(Transaction transaction) {
        return validateTransaction(transaction)
            .flatMap(this::enrichTransactionData)
            .flatMap(this::saveTransaction)
            .timeout(Duration.ofSeconds(processingTimeoutSeconds))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100))
                .filter(throwable -> !(throwable instanceof ValidationException))
            )
            .doOnSuccess(t -> log.debug("Transaction processed successfully: {}", t.getId()))
            .doOnError(error -> log.error("Transaction processing failed: {}", error.getMessage()));
    }

    @Override
    public Mono<Transaction> validateTransaction(Transaction transaction) {
        // Handle null transaction
        if (transaction == null) {
            return Mono.error(new ValidationException("Transaction cannot be null"));
        }

        // Create a composite validation error to collect all validation errors
        List<String> validationErrors = new ArrayList<>();

        return Mono.just(transaction)
            // Validate customer exists
            .flatMap(t -> validateCustomerExists(t.getCustomerId())
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ValidationException("Invalid customer: " + t.getCustomerId()));
                    }
                    return Mono.just(t);
                })
            )
            // Validate product exists and is active
            .flatMap(t -> getProductById(t.getProductCode())
                .switchIfEmpty(Mono.error(new ValidationException("Product not found: " + t.getProductCode())))
                .flatMap(product -> {
                    if (!"ACTIVE".equals(product.getStatus())) {
                        return Mono.error(new ValidationException("Product is not active: " + t.getProductCode()));
                    }

                    // Calculate total cost (for validation only, not stored in entity)
                    BigDecimal totalCost = product.getCost().multiply(BigDecimal.valueOf(t.getQuantity()));

                    // Validate total cost does not exceed 5000
                    if (totalCost.compareTo(BigDecimal.valueOf(5000)) > 0) {
                        return Mono.error(new ValidationException("Total cost cannot exceed 5000. Current total: " + totalCost));
                    }

                    return Mono.just(t);
                })
            )
            // Validate quantity
            .filter(t -> t.getQuantity() > 0)
            .switchIfEmpty(Mono.error(new ValidationException("Quantity must be greater than 0")))
            // Validate transaction date is not in the past
            .filter(t -> t.getTransactionTime() == null || !t.getTransactionTime().isBefore(LocalDateTime.now()))
            .switchIfEmpty(Mono.error(new ValidationException("Transaction date cannot be in the past")))
            .timeout(Duration.ofSeconds(processingTimeoutSeconds))
            .doOnError(error -> log.error("Validation failed: {}", error.getMessage()));
    }

    private Mono<Transaction> enrichTransactionData(Transaction transaction) {
        return Mono.just(transaction)
            .map(t -> {
                t.setProcessedTime(LocalDateTime.now());
                t.setStatus("PROCESSED");
                return t;
            });
    }

    @CachePut(value = "transactionCache", key = "#transaction.id")
    private Mono<Transaction> saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction)
            .timeout(Duration.ofSeconds(processingTimeoutSeconds))
            .doOnSuccess(t -> log.debug("Transaction saved: {}", t.getId()))
            .doOnError(error -> log.error("Failed to save transaction: {}", error.getMessage()));
    }

    private void incrementSuccessMetric(String format) {
        meterRegistry.counter("transaction.success", "format", format.toLowerCase()).increment();
    }

    private void handleProcessingError(Throwable error, String format) {
        log.error("Error processing {} transaction: {}", format, error.getMessage());
        meterRegistry.counter("transaction.error", 
            "format", format.toLowerCase(),
            "error", error.getClass().getSimpleName()).increment();
    }

    public Mono<Transaction> handleTransactionError(Throwable error, Transaction transaction) {
        if (error instanceof TimeoutException) {
            log.error("Transaction processing timeout: {}", transaction.getId());
            meterRegistry.counter("transaction.timeout").increment();
        } else if (error instanceof ValidationException) {
            log.error("Transaction validation failed: {}", transaction.getId());
            meterRegistry.counter("transaction.validation.error").increment();
        }
        return Mono.empty(); // Skip failed transaction and continue processing others
    }

    private static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Cache customer existence check
     * @param customerId Customer ID to check
     * @return Mono<Boolean> indicating if customer exists
     */
    @Cacheable(value = "customerCache", key = "#customerId")
    private Mono<Boolean> validateCustomerExists(Integer customerId) {
        log.debug("Checking customer existence (not from cache): {}", customerId);
        return customerRepository.existsById(customerId);
    }

    /**
     * Cache product retrieval
     * @param productCode Product code to retrieve
     * @return Mono containing the product if found
     */
    @Cacheable(value = "productCache", key = "#productCode")
    private Mono<Product> getProductById(String productCode) {
        log.debug("Retrieving product (not from cache): {}", productCode);
        return productRepository.findById(productCode);
    }

    /**
     * Get totals per customer with caching
     * @return Flux of customer transaction summaries
     */
    @Cacheable(value = "transactionSummaryCache", key = "'customerSummary'")
    public Flux<CustomerTransactionSummaryDTO> getCachedTotalCostPerCustomer() {
        log.debug("Getting total cost per customer (not from cache)");
        return transactionRepository.getTotalCostPerCustomer()
            .doOnNext(record -> log.debug("Received record : {}", record))
            .flatMap(record -> mapper.customerTransactionRecordToDtoReactive(record))
            .doOnNext(dto -> log.debug("Completed fetching and mapping customer transaction summaries : {}", dto))
            .doOnError(error -> log.error("Error while fetching or mapping customer transaction summaries", error));
    }

    /**
     * Get totals per product with caching
     * @return Flux of product transaction summaries
     */
    @Cacheable(value = "transactionSummaryCache", key = "'productSummary'")
    public Flux<ProductTransactionSummaryDTO> getCachedTotalCostPerProduct() {
        log.debug("Getting total cost per product (not from cache)");
        return transactionRepository.getTotalCostPerProduct()
            .doOnNext(record -> log.info("Received record : {}", record))
            .flatMap(record -> mapper.productTransactionRecordToDtoReactive(record))
            .doOnNext(dto -> log.info("Completed fetching and mapping product transaction summaries : {}", dto))
            .doOnError(error -> log.error("Error while fetching or mapping product transaction summaries", error));
    }

}
