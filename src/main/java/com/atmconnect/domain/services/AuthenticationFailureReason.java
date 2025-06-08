package com.atmconnect.domain.services;

/**
 * Enumeration of possible authentication failure reasons.
 * Provides specific failure codes that can be used for logging,
 * monitoring, and providing appropriate user feedback.
 */
public enum AuthenticationFailureReason {
    
    /**
     * Invalid PIN provided by the customer.
     */
    INVALID_PIN("Invalid PIN provided"),
    
    /**
     * Biometric verification failed.
     */
    BIOMETRIC_FAILURE("Biometric verification failed"),
    
    /**
     * Customer account not found in the system.
     */
    CUSTOMER_NOT_FOUND("Customer not found"),
    
    /**
     * Customer account is temporarily locked due to failed attempts.
     */
    ACCOUNT_LOCKED("Account temporarily locked"),
    
    /**
     * Customer account is inactive or disabled.
     */
    ACCOUNT_INACTIVE("Account is inactive"),
    
    /**
     * Device is not registered for this customer.
     */
    DEVICE_NOT_REGISTERED("Device not registered"),
    
    /**
     * Too many authentication attempts detected.
     */
    TOO_MANY_ATTEMPTS("Too many authentication attempts"),
    
    /**
     * Suspicious activity detected during authentication.
     */
    SUSPICIOUS_ACTIVITY("Suspicious activity detected"),
    
    /**
     * Authentication credentials are malformed or invalid.
     */
    INVALID_CREDENTIALS("Invalid credentials format"),
    
    /**
     * Multi-factor authentication failed.
     */
    MFA_FAILURE("Multi-factor authentication failed"),
    
    /**
     * OTP code is invalid or expired.
     */
    INVALID_OTP("Invalid or expired OTP"),
    
    /**
     * Session has expired during authentication.
     */
    SESSION_EXPIRED("Authentication session expired"),
    
    /**
     * System error occurred during authentication.
     */
    SYSTEM_ERROR("Authentication system error"),
    
    /**
     * Rate limiting triggered for this user/IP.
     */
    RATE_LIMITED("Authentication rate limit exceeded"),
    
    /**
     * Biometric authentication is not enabled for this account.
     */
    BIOMETRIC_NOT_ENABLED("Biometric authentication not enabled"),
    
    /**
     * Device fingerprint mismatch detected.
     */
    DEVICE_FINGERPRINT_MISMATCH("Device fingerprint mismatch");
    
    private final String description;
    
    AuthenticationFailureReason(String description) {
        this.description = description;
    }
    
    /**
     * Gets the human-readable description of the failure reason.
     *
     * @return the failure description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this failure reason is security-related.
     *
     * @return true if the failure is due to security concerns
     */
    public boolean isSecurityRelated() {
        return this == ACCOUNT_LOCKED ||
               this == TOO_MANY_ATTEMPTS ||
               this == SUSPICIOUS_ACTIVITY ||
               this == DEVICE_NOT_REGISTERED ||
               this == DEVICE_FINGERPRINT_MISMATCH ||
               this == RATE_LIMITED;
    }
    
    /**
     * Checks if this failure reason allows retry attempts.
     *
     * @return true if authentication can be retried
     */
    public boolean allowsRetry() {
        return this != ACCOUNT_LOCKED &&
               this != CUSTOMER_NOT_FOUND &&
               this != ACCOUNT_INACTIVE &&
               this != RATE_LIMITED;
    }
}