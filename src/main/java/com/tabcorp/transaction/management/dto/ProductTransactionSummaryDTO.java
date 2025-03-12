package com.tabcorp.transaction.management.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTransactionSummaryDTO {
    private String productCode;
    private String status;
    private BigDecimal totalCost;
}


