package com.tabcorp.transaction.management.dto;

public record CustomerTransactionSummaryRecord(
    Long customer_id,
    String first_name,
    String last_name,
    Double total_cost
) {}
