package com.atmconnect.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    
    private String transactionId;
    private String referenceNumber;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String accountId;
    private String atmId;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String message;
    private boolean requiresOtp;
    private BigDecimal availableBalance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionHistoryResponse {
        private List<TransactionResponse> transactions;
        private int totalCount;
        private boolean hasMore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {
        private String accountId;
        private BigDecimal availableBalance;
        private BigDecimal actualBalance;
        private String currency;
        private LocalDateTime lastUpdated;
        private String transactionId;
    }
}