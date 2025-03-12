package com.tabcorp.transaction.management.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MockAuthController {

    // Use a 256-bit (32 character) key for HMAC-SHA256
    private static final String SECRET_KEY = "transaction_management_secret_key_32bytes"; // Must be at least 256 bits (32 characters)

    @PostMapping("/oauth/token")
    public Map<String, String> getToken(@RequestParam String client_id,
                                        @RequestParam String client_secret,
                                        @RequestParam String grant_type) {
        try {
            // Validate client_id, client_secret, and grant_type (mock validation)
            if (!"client-id".equals(client_id) || !"client-secret".equals(client_secret) || !"client_credentials".equals(grant_type)) {
                throw new InvalidClientException("Invalid client credentials or grant type");
            }

        // Create a SecretKey object from the secret key
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        // Generate a JWT token
        String token = Jwts.builder()
            .setSubject("coding-challenge")
            .claim("scope", "api.read")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour expiration
            .signWith(key, SignatureAlgorithm.HS256) // Use the new signWith method
            .compact();

        // Return the token in the response
        Map<String, String> response = new HashMap<>();
        response.put("access_token", token);
        response.put("token_type", "Bearer");
        response.put("expires_in", "3600");
        return response;
        } catch (InvalidClientException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_client");
            errorResponse.put("error_description", e.getMessage());
            throw new InvalidClientException(e.getMessage(), errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "server_error");
            errorResponse.put("error_description", "An unexpected error occurred");
            throw new ServerErrorException("An unexpected error occurred", errorResponse);
        }
    }
    
    // Custom exception classes
    @Getter
    public static class InvalidClientException extends RuntimeException {
        private Map<String, String> errorResponse;
        
        public InvalidClientException(String message) {
            super(message);
        }
        
        public InvalidClientException(String message, Map<String, String> errorResponse) {
            super(message);
            this.errorResponse = errorResponse;
        }

    }
    
    @Getter
    public static class ServerErrorException extends RuntimeException {
        private Map<String, String> errorResponse;
        
        public ServerErrorException(String message, Map<String, String> errorResponse) {
            super(message);
            this.errorResponse = errorResponse;
        }

    }
}
