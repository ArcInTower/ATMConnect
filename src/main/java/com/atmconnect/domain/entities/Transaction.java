package com.atmconnect.domain.entities;

import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Domain entity representing a banking transaction in the ATMConnect system.
 * 
 * <p>This entity encapsulates all information related to a banking transaction,
 * including withdrawals, balance inquiries, transfers, and PIN changes. Each
 * transaction maintains its own security context with verification codes and
 * integrity hashes to ensure secure processing.</p>
 * 
 * <h3>Transaction Lifecycle:</h3>
 * <ol>
 *   <li><strong>PENDING</strong> - Transaction initiated, awaiting verification</li>
 *   <li><strong>COMPLETED</strong> - Transaction successfully processed</li>
 *   <li><strong>FAILED</strong> - Transaction failed due to business rules or system errors</li>
 *   <li><strong>CANCELLED</strong> - Transaction cancelled by user or system</li>
 * </ol>
 * 
 * <h3>Security Features:</h3>
 * <ul>
 *   <li>Unique transaction IDs for tracking and auditing</li>
 *   <li>OTP verification for sensitive operations</li>
 *   <li>Security hash for data integrity verification</li>
 *   <li>Device tracking for multi-factor authentication</li>
 *   <li>Automatic expiration to prevent replay attacks</li>
 * </ul>
 * 
 * @author ATMConnect Development Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "transaction_id", unique = true, nullable = false))
    private TransactionId transactionId;
    
    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    @Column(name = "amount", nullable = false)
    private Money amount;
    
    @Column(name = "currency", nullable = false)
    private String currency;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atm_id")
    private ATM atm;
    
    @Column(name = "device_id")
    private String deviceId;
    
    @Column(name = "otp_code")
    private String otpCode;
    
    @Column(name = "otp_verified")
    private boolean otpVerified;
    
    @Column(name = "reference_number", unique = true)
    private String referenceNumber;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "security_hash", nullable = false)
    private String securityHash;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = new TransactionId();
        }
        status = TransactionStatus.PENDING;
    }
    
    public void complete() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in pending state");
        }
        status = TransactionStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }
    
    public void fail(String errorMessage) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in pending state");
        }
        this.status = TransactionStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
    
    public void cancel() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Only pending transactions can be cancelled");
        }
        status = TransactionStatus.CANCELLED;
        completedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return status == TransactionStatus.PENDING && 
               createdAt.isBefore(LocalDateTime.now().minusMinutes(5));
    }
    
    public enum TransactionType {
        WITHDRAWAL,
        BALANCE_INQUIRY,
        TRANSFER,
        PIN_CHANGE
    }
    
    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}