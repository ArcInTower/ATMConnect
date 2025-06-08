package com.atmconnect.domain.valueobjects;

import lombok.EqualsAndHashCode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@EqualsAndHashCode
public class Pin {
    private static final Pattern PIN_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;
    
    private final String hashedPin;
    private final String salt;
    
    public Pin(String plainPin) {
        validatePin(plainPin);
        this.salt = generateSalt();
        this.hashedPin = hashPin(plainPin, salt);
    }
    
    private Pin(String hashedPin, String salt) {
        this.hashedPin = hashedPin;
        this.salt = salt;
    }
    
    public static Pin fromHash(String hashedPin, String salt) {
        return new Pin(hashedPin, salt);
    }
    
    public boolean verify(String plainPin) {
        if (plainPin == null || !PIN_PATTERN.matcher(plainPin).matches()) {
            return false;
        }
        
        String hashedInput = hashPin(plainPin, salt);
        return MessageDigest.isEqual(hashedPin.getBytes(), hashedInput.getBytes());
    }
    
    private void validatePin(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw new IllegalArgumentException("PIN must be exactly 6 digits");
        }
        
        if (hasSimplePattern(pin)) {
            throw new IllegalArgumentException("PIN is too simple");
        }
    }
    
    private boolean hasSimplePattern(String pin) {
        return pin.matches("(\\d)\\1{5}") ||
               "123456".equals(pin) ||
               "654321".equals(pin) ||
               "111111".equals(pin) ||
               "000000".equals(pin);
    }
    
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[SALT_LENGTH];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }
    
    private String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt.getBytes());
            byte[] hashedBytes = digest.digest(pin.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash PIN", e);
        }
    }
    
    public String getHashedPin() {
        return hashedPin;
    }
    
    public String getSalt() {
        return salt;
    }
}