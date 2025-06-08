package com.atmconnect.domain.services;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Immutable credentials container for authentication operations.
 * Contains various types of authentication data that can be used
 * by different authentication strategies.
 */
@Data
@Builder
public class AuthenticationCredentials {
    
    @NonNull
    private final String customerNumber;
    
    private final String pin;
    private final String biometricData;
    private final String deviceId;
    private final String otpCode;
    private final String sessionToken;
    private final String ipAddress;
    private final String userAgent;
    
    /**
     * Creates credentials for PIN-based authentication.
     *
     * @param customerNumber the customer's identification number
     * @param pin the customer's PIN
     * @return AuthenticationCredentials for PIN authentication
     */
    public static AuthenticationCredentials forPin(String customerNumber, String pin) {
        return AuthenticationCredentials.builder()
                .customerNumber(customerNumber)
                .pin(pin)
                .build();
    }
    
    /**
     * Creates credentials for biometric authentication.
     *
     * @param customerNumber the customer's identification number
     * @param biometricData the biometric verification data
     * @return AuthenticationCredentials for biometric authentication
     */
    public static AuthenticationCredentials forBiometric(String customerNumber, String biometricData) {
        return AuthenticationCredentials.builder()
                .customerNumber(customerNumber)
                .biometricData(biometricData)
                .build();
    }
    
    /**
     * Creates credentials for multi-factor authentication.
     *
     * @param customerNumber the customer's identification number
     * @param pin the customer's PIN
     * @param deviceId the registered device identifier
     * @return AuthenticationCredentials for multi-factor authentication
     */
    public static AuthenticationCredentials forMultiFactor(String customerNumber, String pin, String deviceId) {
        return AuthenticationCredentials.builder()
                .customerNumber(customerNumber)
                .pin(pin)
                .deviceId(deviceId)
                .build();
    }
    
    /**
     * Creates enhanced credentials with additional security context.
     *
     * @param customerNumber the customer's identification number
     * @param pin the customer's PIN
     * @param deviceId the registered device identifier
     * @param ipAddress the client's IP address
     * @param userAgent the client's user agent string
     * @return AuthenticationCredentials with security context
     */
    public static AuthenticationCredentials withSecurityContext(String customerNumber, String pin, 
                                                               String deviceId, String ipAddress, String userAgent) {
        return AuthenticationCredentials.builder()
                .customerNumber(customerNumber)
                .pin(pin)
                .deviceId(deviceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
    }
    
    /**
     * Checks if credentials contain PIN data.
     *
     * @return true if PIN is present and not empty
     */
    public boolean hasPin() {
        return pin != null && !pin.trim().isEmpty();
    }
    
    /**
     * Checks if credentials contain biometric data.
     *
     * @return true if biometric data is present and not empty
     */
    public boolean hasBiometric() {
        return biometricData != null && !biometricData.trim().isEmpty();
    }
    
    /**
     * Checks if credentials contain device information.
     *
     * @return true if device ID is present and not empty
     */
    public boolean hasDevice() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }
    
    /**
     * Checks if credentials contain OTP data.
     *
     * @return true if OTP code is present and not empty
     */
    public boolean hasOtp() {
        return otpCode != null && !otpCode.trim().isEmpty();
    }
}