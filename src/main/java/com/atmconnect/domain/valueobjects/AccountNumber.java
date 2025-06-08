package com.atmconnect.domain.valueobjects;

import lombok.Value;
import java.util.Objects;
import java.util.regex.Pattern;

@Value
public class AccountNumber {
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{10,16}$");
    
    String value;
    
    public AccountNumber(String value) {
        Objects.requireNonNull(value, "Account number cannot be null");
        
        if (!ACCOUNT_NUMBER_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid account number format");
        }
        
        this.value = value;
    }
    
    public String getMasked() {
        if (value.length() <= 4) {
            return "****";
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
    
    @Override
    public String toString() {
        return getMasked();
    }
}