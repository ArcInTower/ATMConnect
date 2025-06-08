package com.atmconnect.domain.services;

/**
 * Enumeration of supported authentication types in the ATMConnect system.
 * Each type represents a different method of verifying customer identity.
 */
public enum AuthenticationType {
    
    /**
     * PIN-based authentication using a 6-digit personal identification number.
     */
    PIN("PIN Authentication", "6-digit PIN verification"),
    
    /**
     * Biometric authentication using fingerprint, facial recognition, or other biometric data.
     */
    BIOMETRIC("Biometric Authentication", "Fingerprint or facial recognition"),
    
    /**
     * Multi-factor authentication combining PIN and device verification.
     */
    MULTI_FACTOR("Multi-Factor Authentication", "PIN + registered device verification"),
    
    /**
     * One-time password authentication for transaction verification.
     */
    OTP("OTP Authentication", "One-time password verification"),
    
    /**
     * Session-based authentication for continued access.
     */
    SESSION("Session Authentication", "Valid session token verification"),
    
    /**
     * Device-based authentication using registered device credentials.
     */
    DEVICE("Device Authentication", "Registered device verification");
    
    private final String displayName;
    private final String description;
    
    AuthenticationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Gets the user-friendly display name for this authentication type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the detailed description of this authentication method.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this authentication type is considered high security.
     *
     * @return true if this is a high-security authentication method
     */
    public boolean isHighSecurity() {
        return this == MULTI_FACTOR || this == BIOMETRIC;
    }
    
    /**
     * Checks if this authentication type requires additional verification.
     *
     * @return true if additional verification steps may be required
     */
    public boolean requiresAdditionalVerification() {
        return this == MULTI_FACTOR || this == OTP;
    }
}