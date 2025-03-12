package com.tabcorp.transaction.management.repository;

import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryRecord;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryRecord;
import com.tabcorp.transaction.management.entity.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends R2dbcRepository<Transaction, Long> {
    
    // High-volume transaction storage
    Flux<Transaction> saveAll(Flux<Transaction> transactions);

    // Total cost of transactions per customer
    @Query("SELECT t.customer_id, c.first_name, c.last_name, SUM(t.quantity * p.cost) as total_cost " +
           "FROM customer_transaction t " +
           "JOIN customer c ON t.customer_id = c.customer_id " +
           "JOIN product p ON t.product_code = p.product_code " +
           "GROUP BY t.customer_id, c.first_name, c.last_name")
    Flux<CustomerTransactionSummaryRecord> getTotalCostPerCustomer();

    // Total cost of transactions per product
    @Query("SELECT t.product_code, p.status, SUM(t.quantity * p.cost) as total_cost " +
           "FROM customer_transaction t " +
           "JOIN product p ON t.product_code = p.product_code " +
           "GROUP BY t.product_code, p.status")
    Flux<ProductTransactionSummaryRecord> getTotalCostPerProduct();

    // Number of transactions for Australian customers
    @Query("SELECT COUNT(*) as transaction_count " +
           "FROM customer_transaction t " +
           "JOIN customer c ON t.customer_id = c.customer_id " +
           "WHERE c.location = 'Australia'")
    Mono<Long> getAustralianTransactionCount();

    // Find transactions by format type (JSON/BSON)
    Flux<Transaction> findByDataFormat(String dataFormat);

}
