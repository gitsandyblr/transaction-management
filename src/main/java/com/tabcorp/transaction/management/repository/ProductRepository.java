package com.tabcorp.transaction.management.repository;

import com.tabcorp.transaction.management.entity.Product;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, String> {
    // Batch insert for products
    @Query("INSERT INTO product (product_code, cost, status) VALUES (:productCode, :cost, :status)")
    Flux<Product> saveAll(Flux<Product> products);

    // Find active products
    Flux<Product> findByStatus(String status);
}

