package com.atmconnect.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {
    
    @NotBlank(message = "Customer ID is required")
    private String customerId;
    
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;
    
    @NotBlank(message = "Public key is required")
    private String publicKey;
    
    private String deviceType;
    private String operatingSystem;
    private String appVersion;
}