package com.atmconnect.domain.ports.outbound;

import com.atmconnect.domain.entities.Transaction;
import com.atmconnect.domain.valueobjects.TransactionId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findByTransactionId(TransactionId transactionId);
    Optional<Transaction> findByReferenceNumber(String referenceNumber);
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId, int limit);
    List<Transaction> findByAccountIdAndCreatedAtBetween(String accountId, LocalDateTime start, LocalDateTime end);
    List<Transaction> findPendingTransactionsOlderThan(LocalDateTime dateTime);
    void deleteById(String id);
}