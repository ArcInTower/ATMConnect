package com.atmconnect.domain.valueobjects;

import lombok.Value;
import java.util.UUID;

@Value
public class TransactionId {
    String value;
    
    public TransactionId() {
        this.value = UUID.randomUUID().toString();
    }
    
    public TransactionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction ID format");
        }
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}