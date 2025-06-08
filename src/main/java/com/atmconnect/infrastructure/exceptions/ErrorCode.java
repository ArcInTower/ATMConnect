package com.atmconnect.infrastructure.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // Authentication Errors (1000-1099)
    INVALID_CREDENTIALS(1001, "Invalid credentials provided", 
        "Invalid customer number or PIN", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1002, "Account is temporarily locked", 
        "Too many failed attempts. Please try again later", HttpStatus.LOCKED),
    DEVICE_NOT_REGISTERED(1003, "Device not registered", 
        "This device is not registered for this account", HttpStatus.FORBIDDEN),
    BIOMETRIC_VERIFICATION_FAILED(1004, "Biometric verification failed", 
        "Biometric verification failed", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(1005, "Session has expired", 
        "Your session has expired. Please login again", HttpStatus.UNAUTHORIZED),
    
    // Transaction Errors (2000-2099)
    INSUFFICIENT_FUNDS(2001, "Insufficient account balance", 
        "Insufficient funds for this transaction", HttpStatus.BAD_REQUEST),
    DAILY_LIMIT_EXCEEDED(2002, "Daily withdrawal limit exceeded", 
        "You have exceeded your daily withdrawal limit", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(2003, "Invalid transaction amount", 
        "Transaction amount is invalid", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND(2004, "Transaction not found", 
        "The requested transaction was not found", HttpStatus.NOT_FOUND),
    TRANSACTION_EXPIRED(2005, "Transaction has expired", 
        "This transaction has expired. Please start a new transaction", HttpStatus.BAD_REQUEST),
    INVALID_OTP(2006, "Invalid OTP provided", 
        "The OTP you entered is incorrect", HttpStatus.BAD_REQUEST),
    TRANSACTION_ALREADY_PROCESSED(2007, "Transaction already processed", 
        "This transaction has already been processed", HttpStatus.CONFLICT),
    
    // ATM Connectivity Errors (3000-3099)
    ATM_NOT_AVAILABLE(3001, "ATM is not available", 
        "The selected ATM is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    ATM_CONNECTION_FAILED(3002, "Failed to connect to ATM", 
        "Unable to connect to ATM. Please try again", HttpStatus.SERVICE_UNAVAILABLE),
    ATM_COMMUNICATION_ERROR(3003, "Communication error with ATM", 
        "Communication error occurred. Please try again", HttpStatus.SERVICE_UNAVAILABLE),
    BLUETOOTH_NOT_ENABLED(3004, "Bluetooth is not enabled", 
        "Please enable Bluetooth to connect to ATM", HttpStatus.BAD_REQUEST),
    ATM_OUT_OF_CASH(3005, "ATM is out of cash", 
        "This ATM is currently out of cash", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Security Errors (4000-4099)
    ENCRYPTION_ERROR(4001, "Encryption/Decryption failed", 
        "A security error occurred. Please try again", HttpStatus.INTERNAL_SERVER_ERROR),
    CERTIFICATE_VALIDATION_FAILED(4002, "Certificate validation failed", 
        "Security certificate validation failed", HttpStatus.FORBIDDEN),
    SIGNATURE_VERIFICATION_FAILED(4003, "Digital signature verification failed", 
        "Message integrity verification failed", HttpStatus.FORBIDDEN),
    SECURITY_PROTOCOL_VIOLATION(4004, "Security protocol violation", 
        "Security protocol violation detected", HttpStatus.FORBIDDEN),
    
    // System Errors (5000-5099)
    INTERNAL_SERVER_ERROR(5001, "Internal server error", 
        "An unexpected error occurred. Please try again later", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(5002, "Database operation failed", 
        "A system error occurred. Please try again later", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR(5003, "External service unavailable", 
        "Service temporarily unavailable. Please try again later", HttpStatus.SERVICE_UNAVAILABLE),
    RATE_LIMIT_EXCEEDED(5004, "Rate limit exceeded", 
        "Too many requests. Please wait before trying again", HttpStatus.TOO_MANY_REQUESTS),
    
    // Validation Errors (6000-6099)
    INVALID_REQUEST_FORMAT(6001, "Invalid request format", 
        "Invalid request format", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(6002, "Missing required field", 
        "Required field is missing", HttpStatus.BAD_REQUEST),
    INVALID_FIELD_VALUE(6003, "Invalid field value", 
        "Invalid field value provided", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(6004, "Account not found", 
        "The specified account was not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND(6005, "Customer not found", 
        "Customer not found", HttpStatus.NOT_FOUND);
    
    private final int code;
    private final String message;
    private final String userMessage;
    private final HttpStatus httpStatus;
}