package com.atmconnect.domain.ports.inbound;

import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.valueobjects.Money;
import com.atmconnect.domain.valueobjects.TransactionId;
import java.util.List;
import java.util.Optional;

public interface TransactionUseCase {
    Transaction initiateWithdrawal(String accountId, Money amount, String atmId, String deviceId);
    Transaction completeWithdrawal(TransactionId transactionId, String otpCode);
    Transaction checkBalance(String accountId, String atmId, String deviceId);
    Transaction initiateTransfer(String fromAccountId, String toAccountId, Money amount, String deviceId);
    Transaction completeTransfer(TransactionId transactionId, String otpCode);
    Optional<Transaction> getTransaction(TransactionId transactionId);
    List<Transaction> getTransactionHistory(String accountId, int limit);
    void cancelTransaction(TransactionId transactionId);
}