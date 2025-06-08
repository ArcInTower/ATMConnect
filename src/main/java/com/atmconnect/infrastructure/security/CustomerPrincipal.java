package com.atmconnect.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.security.Principal;

@Data
@AllArgsConstructor
public class CustomerPrincipal implements Principal {
    private final String customerId;
    private final String deviceId;
    
    @Override
    public String getName() {
        return customerId;
    }
}