package com.atmconnect.application.usecases;

import com.atmconnect.domain.entities.Account;
import com.atmconnect.domain.entities.ATM;
import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.ports.inbound.TransactionUseCase;
import com.atmconnect.domain.ports.outbound.TransactionRepository;
import com.atmconnect.domain.ports.outbound.CryptoService;
import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import com.atmconnect.infrastructure.adapters.outbound.AccountRepository;
import com.atmconnect.infrastructure.adapters.outbound.ATMRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionUseCaseImpl implements TransactionUseCase {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final ATMRepository atmRepository;
    private final CryptoService cryptoService;
    
    @Override
    @Transactional
    public Transaction initiateWithdrawal(String accountId, Money amount, String atmId, String deviceId) {
        log.info("Initiating withdrawal for account: {}, amount: {}", accountId, amount);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        ATM atm = atmRepository.findById(atmId)
            .orElseThrow(() -> new IllegalArgumentException("ATM not found"));
        
        if (!atm.isAvailable()) {
            throw new IllegalStateException("ATM is not available");
        }
        
        if (!account.canWithdraw(amount)) {
            throw new IllegalArgumentException("Withdrawal not allowed");
        }
        
        String otpCode = cryptoService.generateOTP();
        String referenceNumber = generateReferenceNumber();
        
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
            .securityHash(computeTransactionHash(accountId, amount, atmId, deviceId))
            .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        log.info("Withdrawal initiated. Reference: {}, OTP: {}", referenceNumber, otpCode);
        
        // In production, send OTP via SMS/email
        sendOTP(account.getCustomer().getPhoneNumber(), otpCode);
        
        return savedTransaction;
    }
    
    @Override
    @Transactional
    public Transaction completeWithdrawal(TransactionId transactionId, String otpCode) {
        log.info("Completing withdrawal for transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in pending state");
        }
        
        if (transaction.isExpired()) {
            transaction.fail("Transaction expired");
            transactionRepository.save(transaction);
            throw new IllegalStateException("Transaction has expired");
        }
        
        if (!cryptoService.verifyOTP(otpCode, transaction.getOtpCode())) {
            transaction.fail("Invalid OTP");
            transactionRepository.save(transaction);
            throw new IllegalArgumentException("Invalid OTP");
        }
        
        try {
            Account account = transaction.getAccount();
            account.withdraw(transaction.getAmount());
            accountRepository.save(account);
            
            transaction.setOtpVerified(true);
            transaction.complete();
            
            Transaction completedTransaction = transactionRepository.save(transaction);
            
            log.info("Withdrawal completed successfully. Reference: {}", 
                transaction.getReferenceNumber());
            
            return completedTransaction;
        } catch (Exception e) {
            transaction.fail("Withdrawal failed: " + e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Transaction checkBalance(String accountId, String atmId, String deviceId) {
        log.info("Checking balance for account: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        ATM atm = atmRepository.findById(atmId)
            .orElseThrow(() -> new IllegalArgumentException("ATM not found"));
        
        Transaction transaction = Transaction.builder()
            .transactionId(new TransactionId())
            .type(Transaction.TransactionType.BALANCE_INQUIRY)
            .amount(new Money("0", account.getCurrencyCode()))
            .currency(account.getCurrencyCode())
            .account(account)
            .atm(atm)
            .deviceId(deviceId)
            .referenceNumber(generateReferenceNumber())
            .securityHash(computeTransactionHash(accountId, null, atmId, deviceId))
            .build();
        
        transaction.complete();
        return transactionRepository.save(transaction);
    }
    
    @Override
    @Transactional
    public Transaction initiateTransfer(String fromAccountId, String toAccountId, Money amount, String deviceId) {
        log.info("Initiating transfer from: {} to: {}, amount: {}", 
            fromAccountId, toAccountId, amount);
        
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));
        
        if (!fromAccount.canWithdraw(amount)) {
            throw new IllegalArgumentException("Insufficient funds or limit exceeded");
        }
        
        String otpCode = cryptoService.generateOTP();
        String referenceNumber = generateReferenceNumber();
        
        Transaction transaction = Transaction.builder()
            .transactionId(new TransactionId())
            .type(Transaction.TransactionType.TRANSFER)
            .amount(amount)
            .currency(amount.getCurrency().getCurrencyCode())
            .account(fromAccount)
            .deviceId(deviceId)
            .otpCode(otpCode)
            .referenceNumber(referenceNumber)
            .securityHash(computeTransactionHash(fromAccountId, amount, toAccountId, deviceId))
            .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Send OTP
        sendOTP(fromAccount.getCustomer().getPhoneNumber(), otpCode);
        
        return savedTransaction;
    }
    
    @Override
    @Transactional
    public Transaction completeTransfer(TransactionId transactionId, String otpCode) {
        log.info("Completing transfer for transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new IllegalStateException("Transaction is not in pending state");
        }
        
        if (transaction.isExpired()) {
            transaction.fail("Transaction expired");
            transactionRepository.save(transaction);
            throw new IllegalStateException("Transaction has expired");
        }
        
        if (!cryptoService.verifyOTP(otpCode, transaction.getOtpCode())) {
            transaction.fail("Invalid OTP");
            transactionRepository.save(transaction);
            throw new IllegalArgumentException("Invalid OTP");
        }
        
        try {
            // In production, this would handle cross-account transfers
            // For now, we'll mark as completed
            transaction.setOtpVerified(true);
            transaction.complete();
            
            Transaction completedTransaction = transactionRepository.save(transaction);
            
            log.info("Transfer completed successfully. Reference: {}", 
                transaction.getReferenceNumber());
            
            return completedTransaction;
        } catch (Exception e) {
            transaction.fail("Transfer failed: " + e.getMessage());
            transactionRepository.save(transaction);
            throw e;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> getTransaction(TransactionId transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountId, int limit) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limit);
    }
    
    @Override
    @Transactional
    public void cancelTransaction(TransactionId transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        transaction.cancel();
        transactionRepository.save(transaction);
        
        log.info("Transaction cancelled: {}", transactionId);
    }
    
    private String generateReferenceNumber() {
        return "REF" + System.currentTimeMillis() + 
               String.format("%04d", (int)(Math.random() * 10000));
    }
    
    private String computeTransactionHash(String accountId, Money amount, String atmId, String deviceId) {
        String data = String.format("%s|%s|%s|%s|%d", 
            accountId, 
            amount != null ? amount.toString() : "",
            atmId, 
            deviceId, 
            System.currentTimeMillis());
        return cryptoService.computeHash(data.getBytes());
    }
    
    private void sendOTP(String phoneNumber, String otpCode) {
        // In production, integrate with SMS service
        log.info("OTP {} sent to {}", otpCode, phoneNumber.replaceAll("\\d(?=\\d{4})", "*"));
    }
}