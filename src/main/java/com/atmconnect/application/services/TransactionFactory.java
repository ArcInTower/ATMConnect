package com.atmconnect.application.services;

import com.atmconnect.domain.entities.Account;
import com.atmconnect.domain.entities.ATM;
import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import com.atmconnect.domain.ports.outbound.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Factory service for creating different types of transactions.
 * Encapsulates the complex logic of transaction creation and ensures
 * consistent initialization of transaction properties and security attributes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionFactory {
    
    private final CryptoService cryptoService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Creates a new withdrawal transaction with all required security attributes.
     *
     * @param account the account for the withdrawal
     * @param amount the withdrawal amount
     * @param atm the ATM processing the transaction
     * @param deviceId the device initiating the transaction
     * @return a new withdrawal transaction in pending status
     */
    public Transaction createWithdrawalTransaction(Account account, Money amount, ATM atm, String deviceId) {
        log.debug("Creating withdrawal transaction for account: {} at ATM: {}", 
            account.getAccountNumber().getMasked(), atm.getAtmCode());
        
        String otpCode = cryptoService.generateOTP();
        String referenceNumber = generateReferenceNumber();
        String securityHash = computeTransactionHash(account.getId(), amount, atm.getId(), deviceId);
        
        Transaction transaction = Transaction.builder()
                .transactionId(new TransactionId())
                .type(Transaction.TransactionType.WITHDRAWAL)
                .amount(amount)
                .currency(amount.getCurrency().getCurrencyCode())
                .account(account)
                .atm(atm)
                .deviceId(deviceId)
                .otpCode(otpCode)
                .referenceNumber(referenceNumber)
                .securityHash(securityHash)
                .build();
        
        log.info("Withdrawal transaction created with reference: {}", referenceNumber);
        return transaction;
    }
    
    /**
     * Creates a new balance inquiry transaction.
     *
     * @param account the account for the inquiry
     * @param atm the ATM processing the transaction
     * @param deviceId the device initiating the transaction
     * @return a new balance inquiry transaction
     */
    public Transaction createBalanceInquiryTransaction(Account account, ATM atm, String deviceId) {
        log.debug("Creating balance inquiry transaction for account: {} at ATM: {}", 
            account.getAccountNumber().getMasked(), atm.getAtmCode());
        
        Money zeroAmount = new Money("0", account.getCurrencyCode());
        String referenceNumber = generateReferenceNumber();
        String securityHash = computeTransactionHash(account.getId(), null, atm.getId(), deviceId);
        
        Transaction transaction = Transaction.builder()
                .transactionId(new TransactionId())
                .type(Transaction.TransactionType.BALANCE_INQUIRY)
                .amount(zeroAmount)
                .currency(account.getCurrencyCode())
                .account(account)
                .atm(atm)
                .deviceId(deviceId)
                .referenceNumber(referenceNumber)
                .securityHash(securityHash)
                .build();
        
        // Balance inquiries are completed immediately
        transaction.complete();
        
        log.info("Balance inquiry transaction created with reference: {}", referenceNumber);
        return transaction;
    }
    
    /**
     * Creates a new transfer transaction between accounts.
     *
     * @param fromAccount the source account
     * @param toAccountId the destination account ID
     * @param amount the transfer amount
     * @param deviceId the device initiating the transaction
     * @return a new transfer transaction in pending status
     */
    public Transaction createTransferTransaction(Account fromAccount, String toAccountId, 
                                               Money amount, String deviceId) {
        log.debug("Creating transfer transaction from account: {} to account: {}", 
            fromAccount.getAccountNumber().getMasked(), toAccountId);
        
        String otpCode = cryptoService.generateOTP();
        String referenceNumber = generateReferenceNumber();
        String securityHash = computeTransactionHash(fromAccount.getId(), amount, toAccountId, deviceId);
        
        Transaction transaction = Transaction.builder()
                .transactionId(new TransactionId())
                .type(Transaction.TransactionType.TRANSFER)
                .amount(amount)
                .currency(amount.getCurrency().getCurrencyCode())
                .account(fromAccount)
                .deviceId(deviceId)
                .otpCode(otpCode)
                .referenceNumber(referenceNumber)
                .securityHash(securityHash)
                .build();
        
        log.info("Transfer transaction created with reference: {}", referenceNumber);
        return transaction;
    }
    
    /**
     * Creates a new PIN change transaction.
     *
     * @param account the account for the PIN change
     * @param deviceId the device initiating the transaction
     * @return a new PIN change transaction
     */
    public Transaction createPinChangeTransaction(Account account, String deviceId) {
        log.debug("Creating PIN change transaction for account: {}", 
            account.getAccountNumber().getMasked());
        
        String otpCode = cryptoService.generateOTP();
        String referenceNumber = generateReferenceNumber();
        Money zeroAmount = new Money("0", account.getCurrencyCode());
        String securityHash = computeTransactionHash(account.getId(), null, "PIN_CHANGE", deviceId);
        
        Transaction transaction = Transaction.builder()
                .transactionId(new TransactionId())
                .type(Transaction.TransactionType.PIN_CHANGE)
                .amount(zeroAmount)
                .currency(account.getCurrencyCode())
                .account(account)
                .deviceId(deviceId)
                .otpCode(otpCode)
                .referenceNumber(referenceNumber)
                .securityHash(securityHash)
                .build();
        
        log.info("PIN change transaction created with reference: {}", referenceNumber);
        return transaction;
    }
    
    /**
     * Generates a unique reference number for transactions.
     * Format: REF + timestamp + random 4-digit number
     *
     * @return a unique reference number
     */
    private String generateReferenceNumber() {
        long timestamp = System.currentTimeMillis();
        int randomNumber = 1000 + secureRandom.nextInt(9000); // 4-digit random number
        return String.format("REF%d%04d", timestamp, randomNumber);
    }
    
    /**
     * Computes a security hash for transaction integrity verification.
     *
     * @param accountId the account ID
     * @param amount the transaction amount (can be null)
     * @param targetId the target ID (ATM ID, account ID, or operation type)
     * @param deviceId the device ID
     * @return a SHA-256 hash of the transaction parameters
     */
    private String computeTransactionHash(String accountId, Money amount, String targetId, String deviceId) {
        StringBuilder data = new StringBuilder()
                .append(accountId)
                .append("|")
                .append(amount != null ? amount.toString() : "")
                .append("|")
                .append(targetId)
                .append("|")
                .append(deviceId)
                .append("|")
                .append(System.currentTimeMillis());
        
        return cryptoService.computeHash(data.toString().getBytes());
    }
}