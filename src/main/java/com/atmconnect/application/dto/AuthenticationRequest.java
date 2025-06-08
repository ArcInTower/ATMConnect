package com.atmconnect.application.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    
    @NotBlank(message = "Customer number is required")
    @Pattern(regexp = "^[0-9]{8,12}$", message = "Invalid customer number format")
    private String customerNumber;
    
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be exactly 6 digits")
    private String pin;
    
    private String deviceId;
    
    private String biometricData;
    
    @NotBlank(message = "Authentication type is required")
    @Pattern(regexp = "^(PIN|BIOMETRIC|MULTI_FACTOR)$", message = "Invalid authentication type")
    private String authenticationType;
    
    @Size(max = 500, message = "Device info too long")
    private String deviceInfo;
}