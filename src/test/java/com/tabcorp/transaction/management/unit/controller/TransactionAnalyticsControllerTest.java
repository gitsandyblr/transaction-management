package com.tabcorp.transaction.management.unit.controller;

import com.tabcorp.transaction.management.controller.TransactionAnalyticsController;
import com.tabcorp.transaction.management.dto.CustomerTransactionSummaryDTO;
import com.tabcorp.transaction.management.dto.ProductTransactionSummaryDTO;
import com.tabcorp.transaction.management.repository.TransactionRepository;
import com.tabcorp.transaction.management.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionAnalyticsControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionServiceImpl transactionService;

    @InjectMocks
    private TransactionAnalyticsController controller;

    private CustomerTransactionSummaryDTO customerSummary1;
    private CustomerTransactionSummaryDTO customerSummary2;
    private ProductTransactionSummaryDTO productSummary1;
    private ProductTransactionSummaryDTO productSummary2;

    @BeforeEach
    void setUp() {
        // Create test data for customer summaries
        customerSummary1 = new CustomerTransactionSummaryDTO();
        customerSummary1.setCustomerId(10001);
        customerSummary1.setFirstName("Tony");
        customerSummary1.setLastName("Stark");
        customerSummary1.setTotalCost(new BigDecimal("150.00"));

        customerSummary2 = new CustomerTransactionSummaryDTO();
        customerSummary2.setCustomerId(10003);
        customerSummary2.setFirstName("Steve");
        customerSummary2.setLastName("Rogers");
        customerSummary2.setTotalCost(new BigDecimal("200.00"));

        // Create test data for product summaries
        productSummary1 = new ProductTransactionSummaryDTO();
        productSummary1.setProductCode("PRODUCT_001");
        productSummary1.setTotalCost(new BigDecimal("300.00"));

        productSummary2 = new ProductTransactionSummaryDTO();
        productSummary2.setProductCode("PRODUCT_003");
        productSummary2.setTotalCost(new BigDecimal("250.00"));
    }

    @Test
    void testGetTotalCostPerCustomer() {
        // Arrange
        when(transactionService.getCachedTotalCostPerCustomer())
                .thenReturn(Flux.just(customerSummary1, customerSummary2));

        // Act & Assert
        StepVerifier.create(controller.getTotalCostPerCustomer())
                .expectNext(customerSummary1)
                .expectNext(customerSummary2)
                .verifyComplete();

        verify(transactionService, times(1)).getCachedTotalCostPerCustomer();
    }

    @Test
    void testGetTotalCostPerProduct() {
        // Arrange
        when(transactionService.getCachedTotalCostPerProduct())
                .thenReturn(Flux.just(productSummary1, productSummary2));

        // Act & Assert
        StepVerifier.create(controller.getTotalCostPerProduct())
                .expectNext(productSummary1)
                .expectNext(productSummary2)
                .verifyComplete();

        verify(transactionService, times(1)).getCachedTotalCostPerProduct();
    }


    @Test
    void testGetAustralianTransactionCount() {
        // Arrange
        when(transactionRepository.getAustralianTransactionCount())
                .thenReturn(Mono.just(15L));

        // Act & Assert
        StepVerifier.create(controller.getAustralianTransactionCount())
                .expectNextMatches(map -> 
                    map.containsKey("count") && 
                    map.get("count").equals(15L))
                .verifyComplete();

        verify(transactionRepository, times(1)).getAustralianTransactionCount();
    }

}

