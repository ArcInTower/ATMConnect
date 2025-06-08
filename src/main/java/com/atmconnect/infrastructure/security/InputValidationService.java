package com.atmconnect.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class InputValidationService {
    
    // Regex patterns for validation
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9]{10,16}$");
    private static final Pattern CUSTOMER_NUMBER_PATTERN = Pattern.compile("^[0-9]{8,12}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]{8,64}$");
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^[A-Fa-f0-9\\-]{36}$");
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[A-Za-z0-9\\s\\-_.]{1,100}$");
    
    // SQL Injection patterns
    private static final List<String> SQL_INJECTION_PATTERNS = Arrays.asList(
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
        "UNION", "OR", "AND", "--", "/*", "*/", "xp_", "sp_", "exec",
        "'", "\"", ";", "\\\\", "script", "javascript", "onload", "onerror"
    );
    
    // XSS patterns
    private static final List<String> XSS_PATTERNS = Arrays.asList(
        "<script", "</script", "javascript:", "onload=", "onerror=", "onclick=",
        "onfocus=", "onmouseover=", "eval(", "alert(", "confirm(", "prompt("
    );
    
    public boolean isValidAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            return false;
        }
        return ACCOUNT_NUMBER_PATTERN.matcher(accountNumber).matches() && 
               !containsMaliciousContent(accountNumber);
    }
    
    public boolean isValidCustomerNumber(String customerNumber) {
        if (customerNumber == null) {
            return false;
        }
        return CUSTOMER_NUMBER_PATTERN.matcher(customerNumber).matches() && 
               !containsMaliciousContent(customerNumber);
    }
    
    public boolean isValidPin(String pin) {
        if (pin == null) {
            return false;
        }
        return PIN_PATTERN.matcher(pin).matches() && 
               !isWeakPin(pin) && 
               !containsMaliciousContent(pin);
    }
    
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches() && 
               !containsMaliciousContent(phoneNumber);
    }
    
    public boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.length() <= 254 && // RFC 5321 limit\n               EMAIL_PATTERN.matcher(email).matches() && \n               !containsMaliciousContent(email);\n    }\n    \n    public boolean isValidDeviceId(String deviceId) {\n        if (deviceId == null) {\n            return false;\n        }\n        return DEVICE_ID_PATTERN.matcher(deviceId).matches() && \n               !containsMaliciousContent(deviceId);\n    }\n    \n    public boolean isValidTransactionId(String transactionId) {\n        if (transactionId == null) {\n            return false;\n        }\n        return TRANSACTION_ID_PATTERN.matcher(transactionId).matches() && \n               !containsMaliciousContent(transactionId);\n    }\n    \n    public boolean isValidAlphanumeric(String input, int maxLength) {\n        if (input == null || input.length() > maxLength) {\n            return false;\n        }\n        return ALPHANUMERIC_PATTERN.matcher(input).matches() && \n               !containsMaliciousContent(input);\n    }\n    \n    public boolean isValidAmount(String amount) {\n        if (amount == null || amount.trim().isEmpty()) {\n            return false;\n        }\n        \n        try {\n            double value = Double.parseDouble(amount);\n            return value > 0 && value <= 50000 && // Max transaction limit\n                   amount.matches(\"^\\\\d+(\\\\.\\\\d{1,2})?$\") && // Max 2 decimal places\n                   !containsMaliciousContent(amount);\n        } catch (NumberFormatException e) {\n            return false;\n        }\n    }\n    \n    public String sanitizeInput(String input) {\n        if (input == null) {\n            return null;\n        }\n        \n        // Remove potential malicious content\n        String sanitized = input.trim();\n        \n        // HTML encode dangerous characters\n        sanitized = sanitized.replace(\"&\", \"&amp;\")\n                           .replace(\"<\", \"&lt;\")\n                           .replace(\">\", \"&gt;\")\n                           .replace(\"\\\"\", \"&quot;\")\n                           .replace(\"'\", \"&#x27;\");\n        \n        // Remove null bytes\n        sanitized = sanitized.replace(\"\\0\", \"\");\n        \n        // Limit length\n        if (sanitized.length() > 1000) {\n            sanitized = sanitized.substring(0, 1000);\n            log.warn(\"Input truncated due to excessive length\");\n        }\n        \n        return sanitized;\n    }\n    \n    public void validateAndThrow(String input, String fieldName, InputValidator validator) {\n        if (!validator.isValid(input)) {\n            log.warn(\"Invalid input detected for field: {}\", fieldName);\n            throw new IllegalArgumentException(\"Invalid \" + fieldName + \" format\");\n        }\n    }\n    \n    private boolean containsMaliciousContent(String input) {\n        if (input == null) {\n            return false;\n        }\n        \n        String upperInput = input.toUpperCase();\n        \n        // Check for SQL injection patterns\n        for (String pattern : SQL_INJECTION_PATTERNS) {\n            if (upperInput.contains(pattern.toUpperCase())) {\n                log.warn(\"Potential SQL injection attempt detected: {}\", pattern);\n                return true;\n            }\n        }\n        \n        // Check for XSS patterns\n        for (String pattern : XSS_PATTERNS) {\n            if (upperInput.contains(pattern.toUpperCase())) {\n                log.warn(\"Potential XSS attempt detected: {}\", pattern);\n                return true;\n            }\n        }\n        \n        // Check for null bytes and control characters\n        if (input.contains(\"\\0\") || input.matches(\".*[\\\\x00-\\\\x1F\\\\x7F].*\")) {\n            log.warn(\"Control characters detected in input\");\n            return true;\n        }\n        \n        return false;\n    }\n    \n    private boolean isWeakPin(String pin) {\n        if (pin == null || pin.length() != 6) {\n            return true;\n        }\n        \n        // Check for common weak patterns\n        String[] weakPatterns = {\n            \"000000\", \"111111\", \"222222\", \"333333\", \"444444\", \n            \"555555\", \"666666\", \"777777\", \"888888\", \"999999\",\n            \"123456\", \"654321\", \"012345\", \"543210\",\n            \"123123\", \"456456\", \"789789\"\n        };\n        \n        for (String weak : weakPatterns) {\n            if (pin.equals(weak)) {\n                return true;\n            }\n        }\n        \n        // Check for repeated digits\n        if (pin.matches(\"(\\\\d)\\\\1{5}\")) {\n            return true;\n        }\n        \n        // Check for sequential patterns\n        if (isSequential(pin)) {\n            return true;\n        }\n        \n        return false;\n    }\n    \n    private boolean isSequential(String pin) {\n        for (int i = 0; i < pin.length() - 1; i++) {\n            int current = Character.getNumericValue(pin.charAt(i));\n            int next = Character.getNumericValue(pin.charAt(i + 1));\n            \n            if (Math.abs(current - next) != 1) {\n                return false;\n            }\n        }\n        return true;\n    }\n    \n    @FunctionalInterface\n    public interface InputValidator {\n        boolean isValid(String input);\n    }\n}