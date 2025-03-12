package com.tabcorp.transaction.management.controller;

import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import com.tabcorp.transaction.management.repository.TransactionRepository;
import com.tabcorp.transaction.management.service.impl.TransactionServiceImpl;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class TransactionAnalyticsController {

    private final TransactionRepository transactionRepository;
    private final TransactionServiceImpl transactionService;

    /**
     * Get total cost of transactions per customer
     * @return Flux of CustomerTransactionSummaryDTO objects
     */
    @GetMapping(path = "/customer-totals", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed(value = "api.customer.totals", description = "Time taken to retrieve customer transaction totals")
    public Flux<CustomerTransactionSummaryDTO> getTotalCostPerCustomer() {
        log.info("Retrieving total cost per customer");
        return transactionService.getCachedTotalCostPerCustomer()
            .doOnComplete(() -> log.info("Completed retrieving customer transaction totals"))
            .doOnError(error -> log.error("Error retrieving customer transaction totals: {}", error.getMessage()));
    }

    /**
     * Get total cost of transactions per product
     * @return Flux of ProductTransactionSummary objects
     */
    @GetMapping(path = "/product-totals", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed(value = "api.product.totals", description = "Time taken to retrieve product transaction totals")
    public Flux<ProductTransactionSummaryDTO> getTotalCostPerProduct() {
        log.info("Retrieving total cost per product");
        return transactionService.getCachedTotalCostPerProduct()
            .doOnComplete(() -> log.info("Completed retrieving product transaction totals"))
            .doOnError(error -> log.error("Error retrieving product transaction totals: {}", error.getMessage()));
    }

    /**
     * Get number of transactions for Australian customers
     * @return Map containing the count
     */
    @GetMapping(path = "/australia-transactions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed(value = "api.australia.transactions", description = "Time taken to retrieve Australian transaction count")
    public Mono<Map<String, Long>> getAustralianTransactionCount() {
        log.info("Retrieving Australian transaction count");
        return transactionRepository.getAustralianTransactionCount()
            .map(count -> {
                Map<String, Long> result = new HashMap<>();
                result.put("count", count);
                return result;
            })
            .doOnSuccess(count -> log.info("Australian transaction count: {}", count))
            .doOnError(error -> log.error("Error retrieving Australian transaction count: {}", error.getMessage()));
    }
}