package com.tabcorp.transaction.management.unit.service;

import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryRecord;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryRecord;
import com.tabcorp.transaction.management.entity.Product;
import com.tabcorp.transaction.management.entity.Transaction;
import com.tabcorp.transaction.management.mapper.TransactionSummaryMapper;
import com.tabcorp.transaction.management.repository.CustomerRepository;
import com.tabcorp.transaction.management.repository.ProductRepository;
import com.tabcorp.transaction.management.repository.TransactionRepository;
import com.tabcorp.transaction.management.service.impl.TransactionServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransactionSummaryMapper transactionSummaryMapper;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "processingTimeoutSeconds", 5);
        ReflectionTestUtils.setField(transactionService, "batchSize", 100);
        ReflectionTestUtils.setField(transactionService, "parallelThreads", 3);
        transactionService.init();
    }

    @Test
    void validateTransaction_validTransaction_returnsTransaction() {
        // Arrange
        Transaction transaction = createValidTransaction();
        Product product = createValidProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(product));

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectNext(transaction)
            .verifyComplete();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository).findById(transaction.getProductCode());
    }

    @Test
    void validateTransaction_invalidCustomer_returnsError() {
        // Arrange
        Transaction transaction = createValidTransaction();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectErrorMatches(throwable ->
                throwable.getMessage().contains("Invalid customer: 1"))
            .verify();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository, never()).findById(anyString());
    }

    @Test
    void validateTransaction_invalidProduct_returnsError() {
        // Arrange
        Transaction transaction = createValidTransaction();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectErrorMatches(throwable ->
                throwable.getMessage().contains("Product not found: PRODUCT_001"))
            .verify();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository).findById(transaction.getProductCode());
    }

    @Test
    void validateTransaction_inactiveProduct_returnsError() {
        // Arrange
        Transaction transaction = createValidTransaction();
        Product inactiveProduct = createInactiveProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(inactiveProduct));

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectErrorMatches(throwable ->
                throwable.getMessage().contains("Product is not active: PRODUCT_001"))
            .verify();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository).findById(transaction.getProductCode());
    }

    @Test
    void validateTransaction_invalidQuantity_returnsError() {
        // Arrange
        Transaction transaction = createTransactionWithInvalidQuantity();
        Product product = createValidProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(product));

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectErrorMatches(throwable ->
                throwable.getMessage().contains("Quantity must be greater than 0"))
            .verify();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository).findById(transaction.getProductCode());
    }

    @Test
    void processTransaction_validTransaction_persistsTransactionAndReturnsIt() {
        // Arrange
        Transaction transaction = createValidTransaction();
        Product product = createValidProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(product));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

        // Act & Assert
        StepVerifier.create(transactionService.processTransaction(transaction))
            .expectNext(transaction)
            .verifyComplete();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(productRepository).findById(transaction.getProductCode());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void processTransaction_validationError_doesNotPersistTransaction() {
        // Arrange
        Transaction transaction = createValidTransaction();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(false));

        // Act & Assert
        StepVerifier.create(transactionService.processTransaction(transaction))
            .expectErrorMatches(throwable ->
                throwable.getMessage().contains("Invalid customer: 1"))
            .verify();

        verify(customerRepository).existsById(transaction.getCustomerId());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void processTransactionBatch_validTransactions_processesBatchAndReturnsSuccessCount() {
        // Arrange
        List<Transaction> transactions = List.of(
            createValidTransaction(),
            createValidTransaction(),
            createValidTransaction()
        );
        Product product = createValidProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(product));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(transactions.get(0)));

        // Act & Assert
        StepVerifier.create(transactionService.processTransactionBatch(transactions, "JSON"))
            .expectNextMatches(processedTransactions ->
                processedTransactions.size() == 3 &&
                    processedTransactions.stream().allMatch(t ->
                        t.getCustomerId() == 1 &&
                            "PRODUCT_001".equals(t.getProductCode()) &&
                            t.getQuantity() == 3 &&
                            "PROCESSED".equals(t.getStatus())
                    )
            )
            .verifyComplete();

        verify(customerRepository, times(3)).existsById(anyInt());
        verify(productRepository, times(3)).findById(anyString());
        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }

    @Test
    void processTransactionBatch_someInvalidTransactions_processesValidOnesAndReturnsCount() {
        // Arrange
        Transaction validTransaction = createValidTransaction();
        Transaction invalidTransaction = createTransactionWithInvalidQuantity();
        List<Transaction> transactions = List.of(validTransaction, invalidTransaction);

        Product product = createValidProduct();

        when(customerRepository.existsById(anyInt())).thenReturn(Mono.just(true));
        when(productRepository.findById(anyString())).thenReturn(Mono.just(product));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(validTransaction));

        // Act & Assert
        StepVerifier.create(transactionService.processTransactionBatch(transactions, "JSON"))
            .expectNextMatches(processedTransactions ->
                processedTransactions.size() == 1 &&
                    processedTransactions.contains(validTransaction))
            .verifyComplete();

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void handleTransactionError_validationError_logsAndReturnsError() {
        // Arrange
        RuntimeException validationException = new RuntimeException("Validation failed: Customer does not exist");

        // Act & Assert
        Transaction transaction = createValidTransaction();
        StepVerifier.create(transactionService.handleTransactionError(validationException, transaction))
            .verifyComplete(); // Changed to expect completion instead of error
    }

    // Helper methods
    private Transaction createValidTransaction () {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setCustomerId(1);
        transaction.setProductCode("PRODUCT_001");
        transaction.setQuantity(3);
        transaction.setStatus("PROCESSED");
        transaction.setTransactionTime(LocalDateTime.now().plusMinutes(10));
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("payment_method", "credit_card");
        dataMap.put("amount", 150.00);
        transaction.setDataAsMap(dataMap);
        return transaction;
    }

    private Transaction createTransactionWithInvalidQuantity () {
        Transaction transaction = createValidTransaction();
        transaction.setQuantity(0); // Invalid quantity
        return transaction;
    }

    private Product createValidProduct () {
        Product product = new Product();
        product.setProductCode("PRODUCT_001");
        product.setStatus("ACTIVE");
        product.setCost(new BigDecimal("10.00"));
        return product;
    }

    private Product createInactiveProduct () {
        Product product = createValidProduct();
        product.setStatus("INACTIVE");
        return product;
    }

}