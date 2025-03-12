package com.tabcorp.transaction.management.service;

import com.tabcorp.transaction.management.entity.Transaction;
import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

public interface TransactionService {
    /**
     * Process a batch of JSON transactions
     * @param transactions List of transactions to process
     * @return Mono<List<Transaction>> Processed transactions
     */
    Mono<List<Transaction>> processJsonTransactions(List<Transaction> transactions);

    /**
     * Process a batch of BSON transactions
     * @param transactions List of transactions to process
     * @return Mono<List<Transaction>> Processed transactions
     */
    Mono<List<Transaction>> processBsonTransactions(List<Transaction> transactions);

    /**
     * Process a single transaction
     * @param transaction Transaction to process
     * @return Mono<Transaction> Processed transaction
     */
    Mono<Transaction> processTransaction(Transaction transaction);

    /**
     * Validate a transaction
     * @param transaction Transaction to validate
     * @return Mono<Transaction> Validated transaction
     */
    Mono<Transaction> validateTransaction(Transaction transaction);
    
    /**
     * Get cached total cost per customer
     * @return Flux<CustomerTransactionSummaryDTO> Cached total cost per customer
     */
    Flux<CustomerTransactionSummaryDTO> getCachedTotalCostPerCustomer();
    
    /**
     * Get cached total cost per product
     * @return Flux<ProductTransactionSummaryDTO> Cached total cost per product
     */
    Flux<ProductTransactionSummaryDTO> getCachedTotalCostPerProduct();
}
