package com.atmconnect.application.services;

import com.atmconnect.domain.constants.SecurityConstants;
import com.atmconnect.domain.entities.Customer;
import com.atmconnect.domain.services.*;
import com.atmconnect.infrastructure.security.SecurityMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * PIN-based authentication strategy implementation.
 * Handles authentication using customer's 6-digit PIN with security monitoring
 * and rate limiting to prevent brute force attacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PinAuthenticationStrategy implements AuthenticationStrategy {
    
    private final SecurityMonitorService securityMonitor;
    private static final Pattern PIN_PATTERN = Pattern.compile(SecurityConstants.PIN_PATTERN);
    
    @Override
    public AuthenticationResult authenticate(Customer customer, AuthenticationCredentials credentials) {
        log.debug("Attempting PIN authentication for customer: {}", customer.getCustomerNumber());
        
        try {
            validateCredentials(credentials);
            
            // Check if customer account is active and not locked
            if (!customer.isActive()) {
                recordSecurityEvent(credentials, "Inactive account access attempt");
                return AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_INACTIVE,
                    SecurityConstants.INVALID_CREDENTIALS_MESSAGE
                );
            }
            
            if (customer.isLocked()) {
                recordSecurityEvent(credentials, "Locked account access attempt");
                return AuthenticationResult.failure(
                    AuthenticationFailureReason.ACCOUNT_LOCKED,
                    SecurityConstants.ACCOUNT_LOCKED_MESSAGE
                );
            }
            
            // Verify PIN
            if (customer.verifyPin(credentials.getPin())) {
                log.info("PIN authentication successful for customer: {}", customer.getCustomerNumber());
                return AuthenticationResult.success(customer, generateSessionId());
            } else {
                recordFailedAttempt(customer, credentials, "Invalid PIN provided");
                return AuthenticationResult.failure(
                    AuthenticationFailureReason.INVALID_PIN,
                    SecurityConstants.INVALID_CREDENTIALS_MESSAGE
                );
            }
            
        } catch (IllegalStateException e) {
            // Account locked during verification process
            recordSecurityEvent(credentials, "Account locked during PIN verification");
            return AuthenticationResult.failure(
                AuthenticationFailureReason.ACCOUNT_LOCKED,
                SecurityConstants.ACCOUNT_LOCKED_MESSAGE
            );
        } catch (Exception e) {
            log.error("PIN authentication system error for customer: {}", 
                customer.getCustomerNumber(), e);
            recordSecurityEvent(credentials, "System error during PIN authentication");
            return AuthenticationResult.failure(
                AuthenticationFailureReason.SYSTEM_ERROR,
                "Authentication system temporarily unavailable"
            );
        }
    }
    
    @Override
    public AuthenticationType getAuthenticationType() {
        return AuthenticationType.PIN;
    }
    
    @Override
    public boolean canHandle(AuthenticationCredentials credentials) {
        return credentials != null && 
               credentials.hasPin() && 
               credentials.getCustomerNumber() != null;
    }
    
    @Override
    public void validateCredentials(AuthenticationCredentials credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("Authentication credentials cannot be null");
        }
        
        if (credentials.getCustomerNumber() == null || credentials.getCustomerNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer number is required for PIN authentication");
        }
        
        if (!credentials.hasPin()) {
            throw new IllegalArgumentException("PIN is required for PIN authentication");
        }
        
        String pin = credentials.getPin();
        
        // Validate PIN format
        if (!PIN_PATTERN.matcher(pin).matches()) {
            throw new IllegalArgumentException("PIN must be exactly " + 
                SecurityConstants.PIN_LENGTH + " digits");
        }
        
        // Check for weak PIN patterns
        if (isWeakPin(pin)) {
            recordSecurityEvent(credentials, "Weak PIN pattern detected");
            throw new IllegalArgumentException("PIN does not meet security requirements");
        }
    }
    
    /**
     * Checks if the provided PIN matches known weak patterns.
     *
     * @param pin the PIN to validate
     * @return true if the PIN is considered weak
     */
    private boolean isWeakPin(String pin) {
        // Check against known weak patterns
        for (String weakPattern : SecurityConstants.WEAK_PIN_PATTERNS) {
            if (pin.equals(weakPattern)) {
                return true;
            }
        }
        
        // Check for repeated digits (e.g., 111111)
        if (pin.matches("(\\d)\\1{5}")) {
            return true;
        }
        
        // Check for sequential patterns (e.g., 123456, 654321)
        if (isSequentialPin(pin)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if the PIN is a sequential pattern.
     *
     * @param pin the PIN to check
     * @return true if the PIN is sequential
     */
    private boolean isSequentialPin(String pin) {
        boolean ascending = true;
        boolean descending = true;
        
        for (int i = 0; i < pin.length() - 1; i++) {
            int current = Character.getNumericValue(pin.charAt(i));
            int next = Character.getNumericValue(pin.charAt(i + 1));
            
            if (next != current + 1) {
                ascending = false;
            }
            if (next != current - 1) {
                descending = false;
            }
        }
        
        return ascending || descending;
    }
    
    /**
     * Records a failed authentication attempt with security monitoring.
     *
     * @param customer the customer who failed authentication
     * @param credentials the credentials used
     * @param reason the specific reason for failure
     */
    private void recordFailedAttempt(Customer customer, AuthenticationCredentials credentials, String reason) {
        String source = credentials.getIpAddress() != null ? 
            credentials.getIpAddress() : "unknown";
            
        securityMonitor.recordAuthenticationFailure(
            source,
            customer.getCustomerNumber(),
            reason
        );
        
        log.warn("PIN authentication failed for customer: {} - Reason: {}", 
            customer.getCustomerNumber(), reason);
    }
    
    /**
     * Records a security event for monitoring purposes.
     *
     * @param credentials the credentials involved
     * @param details the event details
     */
    private void recordSecurityEvent(AuthenticationCredentials credentials, String details) {
        String source = credentials.getIpAddress() != null ? 
            credentials.getIpAddress() : "unknown";
            
        securityMonitor.recordSecurityEvent(
            SecurityMonitorService.SecurityEventType.AUTHENTICATION_FAILURE,
            SecurityMonitorService.SecurityLevel.MEDIUM,
            source,
            details
        );
    }
    
    /**
     * Generates a unique session identifier for successful authentication.
     *
     * @return a new session ID
     */
    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }
}