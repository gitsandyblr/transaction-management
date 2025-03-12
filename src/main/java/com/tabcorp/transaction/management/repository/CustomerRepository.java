package com.tabcorp.transaction.management.repository;

import com.tabcorp.transaction.management.entity.Customer;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends R2dbcRepository<Customer, Integer> {
    // Batch insert for customers
    @Query("INSERT INTO customer (customer_id, first_name, last_name, age, email, location) VALUES (:customerId, :firstName, :lastName, :age, :email, :location)")
    Flux<Customer> saveAll(Flux<Customer> customers);
    
    // Find by location for analytics
    Flux<Customer> findByLocation(String location);
    
}

