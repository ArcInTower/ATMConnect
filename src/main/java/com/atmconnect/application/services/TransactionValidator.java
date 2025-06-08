package com.atmconnect.application.services;

import com.atmconnect.domain.constants.SecurityConstants;
import com.atmconnect.domain.entities.Account;
import com.atmconnect.domain.entities.ATM;
import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import com.atmconnect.domain.ports.outbound.CryptoService;
import com.atmconnect.infrastructure.exceptions.ATMConnectException;
import com.atmconnect.infrastructure.exceptions.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service responsible for validating transactions and their preconditions.
 * Centralizes validation logic to ensure consistency across transaction operations
 * and provides clear error messages for validation failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionValidator {
    
    private final CryptoService cryptoService;
    
    /**
     * Validates that an account can perform a withdrawal operation.
     *
     * @param account the account to validate
     * @param amount the withdrawal amount
     * @throws ATMConnectException if validation fails
     */
    public void validateWithdrawalEligibility(Account account, Money amount) {
        log.debug("Validating withdrawal eligibility for account: {}", 
            account.getAccountNumber().getMasked());
        
        validateAccountActive(account);
        validateAmount(amount);
        validateSufficientBalance(account, amount);
        validateWithdrawalLimits(account, amount);
    }
    
    /**
     * Validates that an ATM is available for transactions.
     *
     * @param atm the ATM to validate
     * @throws ATMConnectException if ATM is not available
     */
    public void validateAtmAvailability(ATM atm) {
        log.debug("Validating ATM availability: {}", atm.getAtmCode());
        
        if (!atm.isAvailable()) {
            log.warn("ATM not available: {} - Active: {}, Online: {}, Cash: {}", 
                atm.getAtmCode(), atm.isActive(), atm.isOnline(), atm.isCashAvailable());
            throw new ATMConnectException(ErrorCode.ATM_NOT_AVAILABLE);
        }
    }
    
    /**
     * Validates a pending transaction for completion.
     *
     * @param transaction the transaction to validate
     * @param otpCode the OTP code provided for verification
     * @throws ATMConnectException if validation fails
     */
    public void validateTransactionForCompletion(Transaction transaction, String otpCode) {
        log.debug("Validating transaction for completion: {}", 
            transaction.getTransactionId());
        
        validateTransactionStatus(transaction);
        validateTransactionNotExpired(transaction);
        validateOtpCode(transaction, otpCode);
    }
    
    /**
     * Validates that a transaction amount is within acceptable limits.
     *
     * @param amount the amount to validate
     * @throws ATMConnectException if amount is invalid
     */
    public void validateAmount(Money amount) {
        if (amount == null) {
            throw new ATMConnectException(ErrorCode.INVALID_AMOUNT, "Amount cannot be null");
        }
        
        BigDecimal value = amount.getAmount();
        
        if (value.compareTo(BigDecimal.valueOf(SecurityConstants.MIN_TRANSACTION_AMOUNT)) < 0) {
            log.warn("Transaction amount too small: {}", value);
            throw new ATMConnectException(ErrorCode.INVALID_AMOUNT, 
                "Minimum transaction amount is $" + SecurityConstants.MIN_TRANSACTION_AMOUNT);
        }
        
        if (value.compareTo(BigDecimal.valueOf(SecurityConstants.MAX_SINGLE_TRANSACTION_AMOUNT)) > 0) {
            log.warn("Transaction amount too large: {}", value);
            throw new ATMConnectException(ErrorCode.INVALID_AMOUNT, 
                "Maximum transaction amount is $" + SecurityConstants.MAX_SINGLE_TRANSACTION_AMOUNT);
        }
    }
    
    /**
     * Validates device registration for the account.
     *
     * @param account the account to check
     * @param deviceId the device ID to validate
     * @throws ATMConnectException if device is not registered
     */
    public void validateDeviceRegistration(Account account, String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new ATMConnectException(ErrorCode.INVALID_REQUEST_FORMAT, 
                "Device ID is required");
        }
        
        if (!account.getCustomer().isDeviceRegistered(deviceId)) {
            log.warn("Unregistered device access attempt: {} for customer: {}", 
                deviceId, account.getCustomer().getCustomerNumber());
            throw new ATMConnectException(ErrorCode.DEVICE_NOT_REGISTERED);
        }
    }
    
    /**
     * Validates that an account is active and can perform transactions.
     *
     * @param account the account to validate
     * @throws ATMConnectException if account is not active
     */
    private void validateAccountActive(Account account) {
        if (!account.isActive()) {
            log.warn("Inactive account transaction attempt: {}", 
                account.getAccountNumber().getMasked());
            throw new ATMConnectException(ErrorCode.ACCOUNT_NOT_FOUND, 
                "Account is not active");
        }
    }
    
    /**
     * Validates that an account has sufficient balance for the transaction.
     *
     * @param account the account to check
     * @param amount the transaction amount
     * @throws ATMConnectException if insufficient funds
     */
    private void validateSufficientBalance(Account account, Money amount) {
        if (account.getBalance().isLessThan(amount)) {
            log.warn("Insufficient balance for account: {} - Balance: {}, Requested: {}", 
                account.getAccountNumber().getMasked(), 
                account.getBalance().getAmount(), 
                amount.getAmount());
            throw new ATMConnectException(ErrorCode.INSUFFICIENT_FUNDS);
        }
    }
    
    /**
     * Validates withdrawal against daily limits.
     *
     * @param account the account to check
     * @param amount the withdrawal amount
     * @throws ATMConnectException if limits exceeded
     */
    private void validateWithdrawalLimits(Account account, Money amount) {
        if (!account.canWithdraw(amount)) {
            log.warn("Daily withdrawal limit exceeded for account: {} - Limit: {}, Already withdrawn: {}, Requested: {}", 
                account.getAccountNumber().getMasked(),
                account.getDailyWithdrawalLimit().getAmount(),
                account.getDailyWithdrawnAmount().getAmount(),
                amount.getAmount());
            throw new ATMConnectException(ErrorCode.DAILY_LIMIT_EXCEEDED);
        }
    }
    
    /**
     * Validates that a transaction is in pending status.
     *
     * @param transaction the transaction to check
     * @throws ATMConnectException if not in pending status
     */
    private void validateTransactionStatus(Transaction transaction) {
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            log.warn("Transaction not in pending state: {} - Status: {}", 
                transaction.getTransactionId(), transaction.getStatus());
            throw new ATMConnectException(ErrorCode.TRANSACTION_ALREADY_PROCESSED);
        }
    }
    
    /**
     * Validates that a transaction has not expired.
     *
     * @param transaction the transaction to check
     * @throws ATMConnectException if transaction has expired
     */
    private void validateTransactionNotExpired(Transaction transaction) {
        if (transaction.isExpired()) {
            log.warn("Expired transaction access attempt: {}", 
                transaction.getTransactionId());
            throw new ATMConnectException(ErrorCode.TRANSACTION_EXPIRED);
        }
    }
    
    /**
     * Validates the OTP code for a transaction.
     *
     * @param transaction the transaction containing the expected OTP
     * @param providedOtp the OTP code provided by the user
     * @throws ATMConnectException if OTP is invalid
     */
    private void validateOtpCode(Transaction transaction, String providedOtp) {
        if (providedOtp == null || providedOtp.trim().isEmpty()) {
            throw new ATMConnectException(ErrorCode.INVALID_OTP, 
                "OTP code is required");
        }
        
        if (!cryptoService.verifyOTP(providedOtp, transaction.getOtpCode())) {
            log.warn("Invalid OTP provided for transaction: {}", 
                transaction.getTransactionId());
            throw new ATMConnectException(ErrorCode.INVALID_OTP);
        }
    }
}