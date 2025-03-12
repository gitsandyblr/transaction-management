package com.tabcorp.transaction.management.dto;

import org.springframework.data.r2dbc.repository.Query;

import java.math.BigDecimal;

public record ProductTransactionSummaryRecord(
   String product_code,
   String status,
   Double total_cost
) {}
