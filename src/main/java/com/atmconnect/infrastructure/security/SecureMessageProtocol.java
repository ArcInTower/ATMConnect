package com.atmconnect.infrastructure.security;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;

@Data
@Builder
public class SecureMessageProtocol {
    private String messageId;
    private String senderId;
    private String recipientId;
    private long timestamp;
    private String nonce;
    private String encryptedPayload;
    private String signature;
    private int version;
    
    public static final int CURRENT_VERSION = 1;
    private static final long MAX_MESSAGE_AGE_SECONDS = 30;
    
    public boolean isExpired() {
        long currentTime = Instant.now().getEpochSecond();
        return (currentTime - timestamp) > MAX_MESSAGE_AGE_SECONDS;
    }
    
    public byte[] toBytes() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }
    
    public static SecureMessageProtocol fromBytes(byte[] data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(data, SecureMessageProtocol.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize message", e);
        }
    }
    
    public String computeSignatureData() {
        return String.format("%s|%s|%s|%d|%s|%s|%d",
            messageId, senderId, recipientId, timestamp, nonce, encryptedPayload, version);
    }
}