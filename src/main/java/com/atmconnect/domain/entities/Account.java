package com.atmconnect.domain.entities;

import com.atmconnect.domain.valueobjects.AccountNumber;
import com.atmconnect.domain.valueobjects.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Currency;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "account_number", unique = true, nullable = false))
    private AccountNumber accountNumber;
    
    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    
    @Column(name = "balance", nullable = false)
    private Money balance;
    
    @Column(name = "currency", nullable = false)
    private String currencyCode;
    
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    @Column(name = "daily_withdrawal_limit", nullable = false)
    private Money dailyWithdrawalLimit;
    
    @Column(name = "daily_withdrawn_amount", nullable = false)
    private Money dailyWithdrawnAmount;
    
    @Column(name = "last_withdrawal_date")
    private LocalDateTime lastWithdrawalDate;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void withdraw(Money amount) {
        if (!active) {
            throw new IllegalStateException("Account is not active");
        }
        
        if (balance.isLessThan(amount)) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        if (lastWithdrawalDate == null || lastWithdrawalDate.isBefore(today)) {
            dailyWithdrawnAmount = new Money("0", currencyCode);
            lastWithdrawalDate = LocalDateTime.now();
        }
        
        Money totalWithdrawnToday = dailyWithdrawnAmount.add(amount);
        if (totalWithdrawnToday.isGreaterThan(dailyWithdrawalLimit)) {
            throw new IllegalArgumentException("Daily withdrawal limit exceeded");
        }
        
        balance = balance.subtract(amount);
        dailyWithdrawnAmount = totalWithdrawnToday;
        lastWithdrawalDate = LocalDateTime.now();
    }
    
    public void deposit(Money amount) {
        if (!active) {
            throw new IllegalStateException("Account is not active");
        }
        
        balance = balance.add(amount);
    }
    
    public boolean canWithdraw(Money amount) {
        if (!active) {
            return false;
        }
        
        if (balance.isLessThan(amount)) {
            return false;
        }
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        Money effectiveDailyWithdrawn = (lastWithdrawalDate != null && !lastWithdrawalDate.isBefore(today)) 
            ? dailyWithdrawnAmount 
            : new Money("0", currencyCode);
            
        Money totalWithdrawnToday = effectiveDailyWithdrawn.add(amount);
        return !totalWithdrawnToday.isGreaterThan(dailyWithdrawalLimit);
    }
    
    public enum AccountType {
        SAVINGS,
        CHECKING,
        CURRENT
    }
}