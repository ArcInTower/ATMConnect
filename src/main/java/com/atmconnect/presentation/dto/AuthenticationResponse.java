package com.atmconnect.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    
    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private CustomerInfo customer;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private String customerId;
        private String customerNumber;
        private String firstName;
        private String lastName;
        private boolean active;
        private int accountCount;
    }
}