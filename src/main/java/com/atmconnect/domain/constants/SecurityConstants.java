package com.atmconnect.domain.constants;

/**
 * Security-related constants used throughout the ATMConnect application.
 * These constants define security policies, limits, and configurations
 * that ensure consistent behavior across the application.
 */
public final class SecurityConstants {
    
    // Authentication Constants
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 3;
    public static final int ACCOUNT_LOCKOUT_DURATION_MINUTES = 30;
    public static final int PIN_LENGTH = 6;
    public static final int CUSTOMER_NUMBER_MIN_LENGTH = 8;
    public static final int CUSTOMER_NUMBER_MAX_LENGTH = 12;
    public static final int ACCOUNT_NUMBER_MIN_LENGTH = 10;
    public static final int ACCOUNT_NUMBER_MAX_LENGTH = 16;
    
    // Session and Token Constants
    public static final int JWT_DEFAULT_EXPIRATION_MINUTES = 15;
    public static final int REFRESH_TOKEN_EXPIRATION_HOURS = 24;
    public static final int SESSION_TIMEOUT_MINUTES = 15;
    public static final int TOKEN_CLEANUP_INTERVAL_HOURS = 1;
    
    // Transaction Security Constants
    public static final int TRANSACTION_TIMEOUT_MINUTES = 5;
    public static final int OTP_EXPIRATION_MINUTES = 5;
    public static final int OTP_LENGTH = 6;
    public static final double MAX_DAILY_WITHDRAWAL_DEFAULT = 2000.00;
    public static final double MAX_SINGLE_TRANSACTION_AMOUNT = 1000.00;
    public static final double MIN_TRANSACTION_AMOUNT = 0.01;
    
    // Rate Limiting Constants
    public static final int MAX_REQUESTS_PER_MINUTE_GENERAL = 60;
    public static final int MAX_REQUESTS_PER_MINUTE_AUTH = 5;
    public static final int RATE_LIMIT_WINDOW_SECONDS = 60;
    public static final int IP_BLOCK_DURATION_MINUTES = 30;
    
    // Cryptographic Constants
    public static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    public static final String KEY_ALGORITHM = "EC";
    public static final String CURVE_NAME = "secp256r1";
    public static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int GCM_TAG_LENGTH_BITS = 128;
    public static final int GCM_IV_LENGTH_BYTES = 12;
    public static final int AES_KEY_LENGTH_BYTES = 32;
    public static final int SECURE_RANDOM_BYTES = 16;
    public static final int RSA_KEY_SIZE = 2048;
    
    // Bluetooth Constants
    public static final int BLUETOOTH_SCAN_TIMEOUT_SECONDS = 30;
    public static final int BLUETOOTH_CONNECTION_TIMEOUT_SECONDS = 10;
    public static final int BLUETOOTH_MESSAGE_TIMEOUT_SECONDS = 5;
    public static final int MAX_BLUETOOTH_CONNECTIONS = 5;
    public static final int BLUETOOTH_HEARTBEAT_INTERVAL_SECONDS = 60;
    
    // Validation Constants
    public static final int MAX_INPUT_LENGTH = 1000;
    public static final int MAX_PHONE_NUMBER_LENGTH = 15;
    public static final int MAX_EMAIL_LENGTH = 254;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    // Security Monitoring Constants
    public static final int MAX_SECURITY_EVENTS_PER_HOUR = 100;
    public static final int SECURITY_REPORT_INTERVAL_MINUTES = 15;
    public static final int SECURITY_CLEANUP_INTERVAL_HOURS = 1;
    public static final int SECURITY_EVENT_RETENTION_HOURS = 24;
    public static final int MALICIOUS_INPUT_THRESHOLD = 3;
    
    // Database Constants
    public static final int CONNECTION_POOL_MIN_SIZE = 5;
    public static final int CONNECTION_POOL_MAX_SIZE = 20;
    public static final int CONNECTION_TIMEOUT_SECONDS = 30;
    public static final int QUERY_TIMEOUT_SECONDS = 30;
    
    // HTTP and Network Constants
    public static final int HTTP_TIMEOUT_SECONDS = 30;
    public static final int CORS_MAX_AGE_SECONDS = 3600;
    public static final int CSRF_TOKEN_VALIDITY_SECONDS = 3600;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // Error Messages (non-sensitive)
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials provided";
    public static final String ACCOUNT_LOCKED_MESSAGE = "Account temporarily locked due to failed attempts";
    public static final String RATE_LIMIT_MESSAGE = "Too many requests. Please try again later";
    public static final String INSUFFICIENT_FUNDS_MESSAGE = "Insufficient funds for this transaction";
    public static final String TRANSACTION_TIMEOUT_MESSAGE = "Transaction has expired";
    public static final String DEVICE_NOT_REGISTERED_MESSAGE = "Device not registered for this account";
    
    // Regular Expression Patterns
    public static final String ACCOUNT_NUMBER_PATTERN = "^[0-9]{10,16}$";
    public static final String CUSTOMER_NUMBER_PATTERN = "^[0-9]{8,12}$";
    public static final String PIN_PATTERN = "^[0-9]{6}$";
    public static final String PHONE_PATTERN = "^\\+?[1-9]\\d{1,14}$";
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String ALPHANUMERIC_PATTERN = "^[A-Za-z0-9\\s\\-_.]{1,100}$";
    public static final String UUID_PATTERN = "^[A-Fa-f0-9\\-]{36}$";
    public static final String AMOUNT_PATTERN = "^\\d+(\\.\\d{1,2})?$";
    
    // Weak PIN patterns (for validation)
    public static final String[] WEAK_PIN_PATTERNS = {
        "000000", "111111", "222222", "333333", "444444", 
        "555555", "666666", "777777", "888888", "999999",
        "123456", "654321", "012345", "543210",
        "123123", "456456", "789789"
    };
    
    // SQL Injection and XSS patterns for detection
    public static final String[] SQL_INJECTION_KEYWORDS = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
        "UNION", "OR", "AND", "--", "/*", "*/", "xp_", "sp_", "exec"
    };
    
    public static final String[] XSS_PATTERNS = {
        "<script", "</script", "javascript:", "onload=", "onerror=", "onclick=",
        "onfocus=", "onmouseover=", "eval(", "alert(", "confirm(", "prompt("
    };
    
    private SecurityConstants() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}