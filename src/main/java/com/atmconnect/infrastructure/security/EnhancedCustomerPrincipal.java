package com.atmconnect.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.security.Principal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class EnhancedCustomerPrincipal implements Principal {
    private final String customerId;
    private final String deviceId;
    private final String sessionId;
    private final String ipAddress;
    private final long authenticationTime;
    
    public EnhancedCustomerPrincipal(String customerId, String deviceId, String sessionId, String ipAddress) {
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.sessionId = sessionId;
        this.ipAddress = ipAddress;
        this.authenticationTime = Instant.now().getEpochSecond();
    }
    
    @Override
    public String getName() {
        return customerId;
    }
    
    public boolean isSessionExpired(long maxSessionAgeSeconds) {
        return (Instant.now().getEpochSecond() - authenticationTime) > maxSessionAgeSeconds;
    }
}