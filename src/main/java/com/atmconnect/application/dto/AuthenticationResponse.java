package com.atmconnect.application.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    
    private boolean success;
    private String token;
    private String customerId;
    private String customerName;
    private String message;
    private LocalDateTime expiresAt;
    private String refreshToken;
    
    public static AuthenticationResponse success(String token, String customerId, String customerName, LocalDateTime expiresAt) {
        return AuthenticationResponse.builder()
            .success(true)
            .token(token)
            .customerId(customerId)
            .customerName(customerName)
            .expiresAt(expiresAt)
            .message("Authentication successful")
            .build();
    }
    
    public static AuthenticationResponse failure(String message) {
        return AuthenticationResponse.builder()
            .success(false)
            .message(message)
            .build();
    }
}