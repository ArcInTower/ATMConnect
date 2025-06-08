package com.atmconnect.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    
    @NotBlank(message = "Customer number is required")
    @Pattern(regexp = "^[0-9]{8,12}$", message = "Customer number must be 8-12 digits")
    private String customerNumber;
    
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be exactly 6 digits")
    private String pin;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    private String biometricData;
    private String deviceName;
    private String publicKey;
}