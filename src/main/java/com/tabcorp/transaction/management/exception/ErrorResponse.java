package com.tabcorp.transaction.management.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String errorCode; // A custom error code for identifying the type of error
    private String message;   // A human-readable error message
    private int status;       // The HTTP status code
}
