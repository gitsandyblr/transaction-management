package com.tabcorp.transaction.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTransactionSummaryDTO {
    private Integer customerId;
    private String firstName;
    private String lastName;
    private BigDecimal totalCost;
}


