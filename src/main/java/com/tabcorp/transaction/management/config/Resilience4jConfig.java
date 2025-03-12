package com.tabcorp.transaction.management.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                // When 50% of calls fail
            .waitDurationInOpenState(Duration.ofSeconds(10))  // Wait 10 seconds before attempting again
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(100)                  // Consider last 100 calls
            .minimumNumberOfCalls(10)               // Minimum calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(5)  // Number of calls allowed in half-open state
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(50)                // Maximum concurrent calls
            .maxWaitDuration(Duration.ofMillis(500))  // Max wait time for bulkhead
            .build();

        return BulkheadRegistry.of(bulkheadConfig);
    }

    @Bean
    public CircuitBreakerConfig jsonTransactionsCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(40)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(50)
            .build();
    }

    @Bean
    public CircuitBreakerConfig bsonTransactionsCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(40)
            .waitDurationInOpenState(Duration.ofSeconds(5))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(50)
            .build();
    }

    @Bean
    public BulkheadConfig jsonTransactionsBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(40)
            .maxWaitDuration(Duration.ofMillis(200))
            .build();
    }

    @Bean
    public BulkheadConfig bsonTransactionsBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(40)
            .maxWaitDuration(Duration.ofMillis(200))
            .build();
    }
}

