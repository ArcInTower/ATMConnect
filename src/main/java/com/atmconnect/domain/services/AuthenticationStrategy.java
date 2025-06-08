package com.atmconnect.domain.services;

import com.atmconnect.domain.entities.Customer;
import java.util.Optional;

/**
 * Strategy interface for different authentication methods.
 * Implementations provide specific authentication logic for various factors
 * such as PIN, biometric, or multi-factor authentication.
 */
public interface AuthenticationStrategy {
    
    /**
     * Authenticates a customer using the specific strategy.
     *
     * @param customer the customer to authenticate
     * @param credentials the authentication credentials (varies by strategy)
     * @return AuthenticationResult containing success status and details
     */
    AuthenticationResult authenticate(Customer customer, AuthenticationCredentials credentials);
    
    /**
     * Returns the type of authentication this strategy handles.
     *
     * @return the authentication type
     */
    AuthenticationType getAuthenticationType();
    
    /**
     * Checks if the strategy can handle the given credentials.
     *
     * @param credentials the credentials to validate
     * @return true if the strategy can process these credentials
     */
    boolean canHandle(AuthenticationCredentials credentials);
    
    /**
     * Validates the format and basic requirements of credentials
     * before attempting authentication.
     *
     * @param credentials the credentials to validate
     * @throws IllegalArgumentException if credentials are invalid
     */
    void validateCredentials(AuthenticationCredentials credentials);
}