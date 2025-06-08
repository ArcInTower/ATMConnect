package com.atmconnect.domain.services;

import com.atmconnect.domain.entities.Customer;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable result object for authentication operations.
 * Contains the outcome of authentication attempts with detailed
 * information about success, failure reasons, and security context.
 */
@Data
@Builder
public class AuthenticationResult {
    
    @NonNull
    private final boolean successful;
    
    private final Customer authenticatedCustomer;
    private final AuthenticationFailureReason failureReason;
    private final String failureMessage;
    private final LocalDateTime timestamp;
    private final String sessionId;
    private final Map<String, Object> securityContext;
    private final boolean requiresAdditionalVerification;
    private final String nextStepInstructions;
    
    /**
     * Creates a successful authentication result.
     *
     * @param customer the successfully authenticated customer
     * @param sessionId the generated session identifier
     * @return AuthenticationResult indicating success
     */
    public static AuthenticationResult success(Customer customer, String sessionId) {
        return AuthenticationResult.builder()
                .successful(true)
                .authenticatedCustomer(customer)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a successful authentication result with additional context.
     *
     * @param customer the successfully authenticated customer
     * @param sessionId the generated session identifier
     * @param securityContext additional security information
     * @return AuthenticationResult indicating success with context
     */
    public static AuthenticationResult successWithContext(Customer customer, String sessionId, 
                                                         Map<String, Object> securityContext) {
        return AuthenticationResult.builder()
                .successful(true)
                .authenticatedCustomer(customer)
                .sessionId(sessionId)
                .securityContext(securityContext)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a failed authentication result.
     *
     * @param reason the specific reason for authentication failure
     * @param message human-readable failure message
     * @return AuthenticationResult indicating failure
     */
    public static AuthenticationResult failure(AuthenticationFailureReason reason, String message) {
        return AuthenticationResult.builder()
                .successful(false)
                .failureReason(reason)
                .failureMessage(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a result indicating additional verification is required.
     *
     * @param message instructions for the next step
     * @return AuthenticationResult indicating additional verification needed
     */
    public static AuthenticationResult requiresVerification(String message) {
        return AuthenticationResult.builder()
                .successful(false)
                .requiresAdditionalVerification(true)
                .nextStepInstructions(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Gets the authenticated customer if authentication was successful.
     *
     * @return Optional containing the customer, or empty if authentication failed
     */
    public Optional<Customer> getCustomer() {
        return successful ? Optional.ofNullable(authenticatedCustomer) : Optional.empty();
    }
    
    /**
     * Checks if the authentication failure was due to security reasons.
     *
     * @return true if failure was security-related
     */
    public boolean isSecurityFailure() {
        return !successful && failureReason != null && 
               (failureReason == AuthenticationFailureReason.ACCOUNT_LOCKED ||
                failureReason == AuthenticationFailureReason.SUSPICIOUS_ACTIVITY ||
                failureReason == AuthenticationFailureReason.DEVICE_NOT_REGISTERED ||
                failureReason == AuthenticationFailureReason.TOO_MANY_ATTEMPTS);
    }
    
    /**
     * Checks if the authentication can be retried.
     *
     * @return true if retry is allowed
     */
    public boolean canRetry() {
        return !successful && failureReason != null &&
               failureReason != AuthenticationFailureReason.ACCOUNT_LOCKED &&
               failureReason != AuthenticationFailureReason.CUSTOMER_NOT_FOUND;
    }
}