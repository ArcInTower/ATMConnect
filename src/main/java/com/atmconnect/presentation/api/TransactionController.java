package com.atmconnect.presentation.api;

import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.ports.inbound.TransactionUseCase;
import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import com.atmconnect.presentation.dto.TransactionRequest;
import com.atmconnect.presentation.dto.TransactionResponse;
import com.atmconnect.presentation.dto.ApiErrorResponse;
import com.atmconnect.infrastructure.exceptions.ATMConnectException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST API controller for transaction operations.
 * 
 * Provides endpoints for:
 * - Cash withdrawals
 * - Balance inquiries
 * - Money transfers
 * - Transaction history
 * - Transaction status checking
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://atmconnect.bank", "https://mobile.atmconnect.bank"})
public class TransactionController {
    
    private final TransactionUseCase transactionUseCase;
    
    /**
     * Initiates a cash withdrawal transaction.
     */
    @PostMapping("/withdraw/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiateWithdrawal(@Valid @RequestBody TransactionRequest request) {
        log.info("Withdrawal initiation request for account: {}, amount: {}", 
                maskAccountId(request.getAccountId()), request.getAmount());
        
        try {
            Money amount = new Money(request.getAmount().toString(), Currency.getInstance(request.getCurrency()));
            
            Transaction transaction = transactionUseCase.initiateWithdrawal(
                request.getAccountId(),
                amount,
                request.getAtmId(),
                request.getDeviceId()
            );
            
            TransactionResponse response = mapToTransactionResponse(transaction);
            response.setRequiresOtp(true);
            response.setMessage("Withdrawal initiated. Please enter the OTP sent to your registered mobile number.");
            
            log.info("Withdrawal initiated successfully. Reference: {}", transaction.getReferenceNumber());
            
            return ResponseEntity.ok(response);
            
        } catch (ATMConnectException e) {
            log.warn("Withdrawal initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(e.getErrorCode().name(), 
                                        e.getMessage(), 
                                        "/api/v1/transactions/withdraw/initiate", 
                                        400));
        } catch (Exception e) {
            log.error("Withdrawal initiation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("WITHDRAWAL_INITIATION_FAILED", 
                                        "Unable to initiate withdrawal", 
                                        "/api/v1/transactions/withdraw/initiate", 
                                        500));
        }
    }
    
    /**
     * Completes a withdrawal transaction with OTP verification.
     */
    @PostMapping("/withdraw/complete")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> completeWithdrawal(@Valid @RequestBody TransactionRequest request) {
        log.info("Withdrawal completion request for transaction: {}", request.getTransactionId());
        
        try {
            TransactionId transactionId = new TransactionId(request.getTransactionId());
            
            Transaction transaction = transactionUseCase.completeWithdrawal(transactionId, request.getOtpCode());
            
            TransactionResponse response = mapToTransactionResponse(transaction);
            response.setMessage("Withdrawal completed successfully. Please collect your cash.");
            
            log.info("Withdrawal completed successfully. Reference: {}", transaction.getReferenceNumber());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Withdrawal completion failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("INVALID_OTP", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/withdraw/complete", 
                                        400));
        } catch (IllegalStateException e) {
            log.warn("Withdrawal completion failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("TRANSACTION_STATE_ERROR", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/withdraw/complete", 
                                        400));
        } catch (Exception e) {
            log.error("Withdrawal completion error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("WITHDRAWAL_COMPLETION_FAILED", 
                                        "Unable to complete withdrawal", 
                                        "/api/v1/transactions/withdraw/complete", 
                                        500));
        }
    }
    
    /**
     * Checks account balance.
     */
    @PostMapping("/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> checkBalance(@Valid @RequestBody TransactionRequest request) {
        log.info("Balance inquiry for account: {}", maskAccountId(request.getAccountId()));
        
        try {
            Transaction transaction = transactionUseCase.checkBalance(
                request.getAccountId(),
                request.getAtmId(),
                request.getDeviceId()
            );
            
            // Extract balance from account (would need to implement this properly)
            TransactionResponse.BalanceResponse response = TransactionResponse.BalanceResponse.builder()
                .accountId(request.getAccountId())
                .availableBalance(transaction.getAccount().getAvailableBalance().getAmount())
                .actualBalance(transaction.getAccount().getBalance().getAmount())
                .currency(transaction.getCurrency())
                .lastUpdated(transaction.getCreatedAt())
                .transactionId(transaction.getTransactionId().getValue())
                .build();
            
            log.info("Balance inquiry completed for account: {}", maskAccountId(request.getAccountId()));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("ACCOUNT_NOT_FOUND", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/balance", 
                                        400));
        } catch (Exception e) {
            log.error("Balance inquiry error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("BALANCE_INQUIRY_FAILED", 
                                        "Unable to check balance", 
                                        "/api/v1/transactions/balance", 
                                        500));
        }
    }
    
    /**
     * Initiates a money transfer between accounts.
     */
    @PostMapping("/transfer/initiate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> initiateTransfer(@Valid @RequestBody TransactionRequest request) {
        log.info("Transfer initiation from account: {} to account: {}", 
                maskAccountId(request.getAccountId()), 
                maskAccountId(request.getToAccountId()));
        
        try {
            Money amount = new Money(request.getAmount().toString(), Currency.getInstance(request.getCurrency()));
            
            Transaction transaction = transactionUseCase.initiateTransfer(
                request.getAccountId(),
                request.getToAccountId(),
                amount,
                request.getDeviceId()
            );
            
            TransactionResponse response = mapToTransactionResponse(transaction);
            response.setRequiresOtp(true);
            response.setMessage("Transfer initiated. Please enter the OTP sent to your registered mobile number.");
            
            log.info("Transfer initiated successfully. Reference: {}", transaction.getReferenceNumber());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("TRANSFER_VALIDATION_ERROR", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/transfer/initiate", 
                                        400));
        } catch (Exception e) {
            log.error("Transfer initiation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("TRANSFER_INITIATION_FAILED", 
                                        "Unable to initiate transfer", 
                                        "/api/v1/transactions/transfer/initiate", 
                                        500));
        }
    }
    
    /**
     * Completes a money transfer with OTP verification.
     */
    @PostMapping("/transfer/complete")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> completeTransfer(@Valid @RequestBody TransactionRequest request) {
        log.info("Transfer completion request for transaction: {}", request.getTransactionId());
        
        try {
            TransactionId transactionId = new TransactionId(request.getTransactionId());
            
            Transaction transaction = transactionUseCase.completeTransfer(transactionId, request.getOtpCode());
            
            TransactionResponse response = mapToTransactionResponse(transaction);
            response.setMessage("Transfer completed successfully.");
            
            log.info("Transfer completed successfully. Reference: {}", transaction.getReferenceNumber());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("INVALID_OTP", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/transfer/complete", 
                                        400));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("TRANSACTION_STATE_ERROR", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/transfer/complete", 
                                        400));
        } catch (Exception e) {
            log.error("Transfer completion error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("TRANSFER_COMPLETION_FAILED", 
                                        "Unable to complete transfer", 
                                        "/api/v1/transactions/transfer/complete", 
                                        500));
        }
    }
    
    /**
     * Retrieves transaction history for an account.
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Transaction history request for account: {}", maskAccountId(accountId));
        
        try {
            List<Transaction> transactions = transactionUseCase.getTransactionHistory(accountId, limit);
            
            List<TransactionResponse> transactionResponses = transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
            
            TransactionResponse.TransactionHistoryResponse response = 
                TransactionResponse.TransactionHistoryResponse.builder()
                    .transactions(transactionResponses)
                    .totalCount(transactionResponses.size())
                    .hasMore(transactionResponses.size() == limit)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Transaction history error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("HISTORY_RETRIEVAL_FAILED", 
                                        "Unable to retrieve transaction history", 
                                        "/api/v1/transactions/history", 
                                        500));
        }
    }
    
    /**
     * Retrieves details of a specific transaction.
     */
    @GetMapping("/{transactionId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getTransaction(@PathVariable String transactionId) {
        log.info("Transaction detail request for: {}", transactionId);
        
        try {
            TransactionId txnId = new TransactionId(transactionId);
            Optional<Transaction> transactionOpt = transactionUseCase.getTransaction(txnId);
            
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            TransactionResponse response = mapToTransactionResponse(transactionOpt.get());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Transaction retrieval error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("TRANSACTION_RETRIEVAL_FAILED", 
                                        "Unable to retrieve transaction", 
                                        "/api/v1/transactions/" + transactionId, 
                                        500));
        }
    }
    
    /**
     * Cancels a pending transaction.
     */
    @PostMapping("/{transactionId}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelTransaction(@PathVariable String transactionId) {
        log.info("Transaction cancellation request for: {}", transactionId);
        
        try {
            TransactionId txnId = new TransactionId(transactionId);
            transactionUseCase.cancelTransaction(txnId);
            
            return ResponseEntity.ok()
                .body("{\"success\": true, \"message\": \"Transaction cancelled successfully\"}");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("TRANSACTION_NOT_FOUND", 
                                        e.getMessage(), 
                                        "/api/v1/transactions/" + transactionId + "/cancel", 
                                        400));
        } catch (Exception e) {
            log.error("Transaction cancellation error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("CANCELLATION_FAILED", 
                                        "Unable to cancel transaction", 
                                        "/api/v1/transactions/" + transactionId + "/cancel", 
                                        500));
        }
    }
    
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
            .transactionId(transaction.getTransactionId().getValue())
            .referenceNumber(transaction.getReferenceNumber())
            .type(transaction.getType().name())
            .status(transaction.getStatus().name())
            .amount(transaction.getAmount().getAmount())
            .currency(transaction.getCurrency())
            .accountId(transaction.getAccount().getId())
            .atmId(transaction.getAtm() != null ? transaction.getAtm().getId() : null)
            .createdAt(transaction.getCreatedAt())
            .completedAt(transaction.getCompletedAt())
            .availableBalance(transaction.getAccount().getAvailableBalance().getAmount())
            .build();
    }
    
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() <= 4) {
            return "****";
        }
        return "****" + accountId.substring(accountId.length() - 4);
    }
}