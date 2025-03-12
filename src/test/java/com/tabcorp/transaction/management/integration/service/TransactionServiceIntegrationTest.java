package com.tabcorp.transaction.management.integration.service;

import com.tabcorp.transaction.management.entity.Customer;
import com.tabcorp.transaction.management.entity.Product;
import com.tabcorp.transaction.management.entity.Transaction;
import com.tabcorp.transaction.management.integration.config.TestCacheConfiguration;
import com.tabcorp.transaction.management.repository.CustomerRepository;
import com.tabcorp.transaction.management.repository.ProductRepository;
import com.tabcorp.transaction.management.repository.TransactionRepository;
import com.tabcorp.transaction.management.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest(classes = {TestCacheConfiguration.class})
@ActiveProfiles("test")
public class TransactionServiceIntegrationTest {
    
    // Static counter for generating unique transaction IDs
    private static long transactionIdCounter = 1;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DatabaseClient databaseClient;

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceIntegrationTest.class);

    // Test data
    private Customer testCustomer1;
    private Customer testCustomer2;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    @Order(2)
    void setUp() {
        // Create customer objects for test references
        testCustomer1 = new Customer();
        testCustomer1.setCustomerId(101);
        testCustomer1.setFirstName("Test");
        testCustomer1.setLastName("Customer 1");
        testCustomer1.setEmail("customer1@test.com");
        testCustomer1.setAge(30);
        testCustomer1.setLocation("Sydney");

        testCustomer2 = new Customer();
        testCustomer2.setCustomerId(102);
        testCustomer2.setFirstName("Test");
        testCustomer2.setLastName("Customer 2");
        testCustomer2.setEmail("customer2@test.com");
        testCustomer2.setAge(25);
        testCustomer2.setLocation("Melbourne");

        // Create product objects for test references
        testProduct1 = new Product();
        testProduct1.setProductCode("TEST_PROD_001");
        testProduct1.setCost(BigDecimal.valueOf(10.50));
        testProduct1.setStatus("ACTIVE");

        testProduct2 = new Product();
        testProduct2.setProductCode("TEST_PROD_002");
        testProduct2.setCost(BigDecimal.valueOf(25.75));
        testProduct2.setStatus("ACTIVE");
        try {
            // Insert customers directly with SQL to avoid update issues
            databaseClient.sql("INSERT INTO customer (customer_id, first_name, last_name, email, age, location) VALUES (:id, :firstName, :lastName, :email, :age, :location)")
                .bind("id", testCustomer1.getCustomerId())
                .bind("firstName", testCustomer1.getFirstName())
                .bind("lastName", testCustomer1.getLastName())
                .bind("email", testCustomer1.getEmail())
                .bind("age", testCustomer1.getAge())
                .bind("location", testCustomer1.getLocation())
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Inserted customer1 successfully"))
                .doOnError(error -> logger.error("Failed to insert customer1: {}", error.getMessage()))
                .block();
            databaseClient.sql("INSERT INTO customer (customer_id, first_name, last_name, email, age, location) VALUES (:id, :firstName, :lastName, :email, :age, :location)")
                .bind("id", testCustomer2.getCustomerId())
                .bind("firstName", testCustomer2.getFirstName())
                .bind("lastName", testCustomer2.getLastName())
                .bind("email", testCustomer2.getEmail())
                .bind("age", testCustomer2.getAge())
                .bind("location", testCustomer2.getLocation())
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Inserted customer2 successfully"))
                .doOnError(error -> logger.error("Failed to insert customer2: {}", error.getMessage()))
                .block();

            // Insert products directly with SQL to avoid update issues
            databaseClient.sql("INSERT INTO product (product_code, cost, status) VALUES (:code, :cost, :status)")
                .bind("code", testProduct1.getProductCode())
                .bind("cost", testProduct1.getCost())
                .bind("status", testProduct1.getStatus())
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Inserted product1 successfully"))
                .doOnError(error -> logger.error("Failed to insert product1: {}", error.getMessage()))
                .block();
            databaseClient.sql("INSERT INTO product (product_code, cost, status) VALUES (:code, :cost, :status)")
                .bind("code", testProduct2.getProductCode())
                .bind("cost", testProduct2.getCost())
                .bind("status", testProduct2.getStatus())
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Inserted product2 successfully"))
                .doOnError(error -> logger.error("Failed to insert product2: {}", error.getMessage()))
                .block();

            logger.info("Test data inserted successfully using SQL");
        } catch (Exception e) {
            logger.error("Error inserting test data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to set up test data", e);
        }
    }
    
    @BeforeEach
    @Order(1)
    void cleanupDatabase() {
        // Clean up database using direct SQL to ensure complete cleanup
        logger.info("Starting database cleanup");
        
        // Use reactive approach with proper error handling
        try {
            // Order matters here - delete transactions first to avoid constraint violations
            databaseClient.sql("DELETE FROM customer_transaction")
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Deleted {} rows from customer_transaction table", count))
                .doOnError(error -> logger.error("Failed to delete from customer_transaction table: {}", error.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error deleting from customer_transaction table: {}", e.getMessage(), e);
                    return Mono.just(0L);
                })
                .block();
                
            databaseClient.sql("DELETE FROM customer")
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Deleted {} rows from customer table", count))
                .doOnError(error -> logger.error("Failed to delete from customer table: {}", error.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error deleting from customer table: {}", e.getMessage(), e);
                    return Mono.just(0L);
                })
                .block();
                
            databaseClient.sql("DELETE FROM product")
                .fetch()
                .rowsUpdated()
                .doOnSuccess(count -> logger.info("Deleted {} rows from product table", count))
                .doOnError(error -> logger.error("Failed to delete from product table: {}", error.getMessage()))
                .onErrorResume(e -> {
                    logger.error("Error deleting from product table: {}", e.getMessage(), e);
                    return Mono.just(0L);
                })
                .block();
            
            logger.info("Database cleared successfully with direct SQL");
        } catch (Exception e) {
            logger.error("Error during database cleanup: {}", e.getMessage(), e);
        }
    }

    @Test
    void processTransaction_invalidCustomer_returnsError() {
        // Arrange
        Transaction transaction = createValidTransaction(999, testProduct1.getProductCode(), 3);

        // Act & Assert
        StepVerifier.create(transactionService.processTransaction(transaction))
            .expectErrorMatches(throwable -> 
                throwable.getMessage().contains("Invalid customer: 999")
            )
            .verify();

        // Verify no transaction was saved
        StepVerifier.create(transactionRepository.count())
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    void processTransaction_invalidProduct_returnsError() {
        // Arrange
        Transaction transaction = createValidTransaction(testCustomer1.getCustomerId(), "INVALID_PRODUCT", 3);

        // Act & Assert
        StepVerifier.create(transactionService.processTransaction(transaction))
            .expectErrorMatches(throwable -> 
                throwable.getMessage().contains("Product not found: INVALID_PRODUCT")
            )
            .verify();

        // Verify no transaction was saved
        StepVerifier.create(transactionRepository.count())
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    void validateTransaction_validTransaction_returnsTransaction() {
        // Arrange
        Transaction transaction = createValidTransaction(testCustomer1.getCustomerId(), testProduct1.getProductCode(), 3);

        // Act & Assert
        StepVerifier.create(transactionService.validateTransaction(transaction))
            .expectNextMatches(tx -> 
                tx.getCustomerId() == testCustomer1.getCustomerId() &&
                tx.getProductCode().equals(testProduct1.getProductCode()) &&
                tx.getQuantity() == 3
            )
            .verifyComplete();
    }

    // Helper method to create a valid transaction for testing
    private Transaction createValidTransaction(int customerId, String productCode, int quantity) {
        Transaction transaction = new Transaction();
        transaction.setId(transactionIdCounter++); // Increment counter for unique IDs
        transaction.setCustomerId(customerId);
        transaction.setProductCode(productCode);
        transaction.setQuantity(quantity);
        transaction.setTransactionTime(LocalDateTime.now().plusMinutes(10)); // Ensure it's not in the past
        transaction.setDataFormat("JSON"); // Set default dataFormat to JSON
        // Initialize empty JSON data
        transaction.setJsonData("{}");
        return transaction;
    }
}

