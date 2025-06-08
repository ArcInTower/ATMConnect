package com.atmconnect.presentation.api;

import com.atmconnect.domain.entities.Customer;
import com.atmconnect.domain.ports.inbound.AuthenticationUseCase;
import com.atmconnect.infrastructure.security.JwtTokenProvider;
import com.atmconnect.presentation.dto.AuthenticationRequest;
import com.atmconnect.presentation.dto.AuthenticationResponse;
import com.atmconnect.presentation.dto.DeviceRegistrationRequest;
import com.atmconnect.presentation.dto.ApiErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST API controller for authentication operations.
 * 
 * Provides endpoints for:
 * - PIN-based authentication
 * - Biometric authentication  
 * - Multi-factor authentication
 * - Device registration
 * - Session logout
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"https://atmconnect.bank", "https://mobile.atmconnect.bank"})
public class AuthenticationController {
    
    private final AuthenticationUseCase authenticationUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    
    /**
     * Authenticates user with PIN and device ID.
     */
    @PostMapping("/login/pin")
    public ResponseEntity<?> authenticateWithPin(@Valid @RequestBody AuthenticationRequest request) {
        log.info("PIN authentication request for customer: {}", 
                maskCustomerNumber(request.getCustomerNumber()));
        
        try {
            Optional<Customer> customerOpt = authenticationUseCase.authenticateWithMultiFactor(
                request.getCustomerNumber(), 
                request.getPin(), 
                request.getDeviceId()
            );
            
            if (customerOpt.isEmpty()) {
                log.warn("Authentication failed for customer: {}", 
                        maskCustomerNumber(request.getCustomerNumber()));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of("AUTHENTICATION_FAILED", 
                                            "Invalid credentials or device not registered", 
                                            "/api/v1/auth/login/pin", 
                                            401));
            }
            
            Customer customer = customerOpt.get();
            String token = jwtTokenProvider.generateToken(customer.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(customer.getId());
            
            AuthenticationResponse response = AuthenticationResponse.builder()
                .success(true)
                .message("Authentication successful")
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .customer(AuthenticationResponse.CustomerInfo.builder()
                    .customerId(customer.getId())
                    .customerNumber(customer.getCustomerNumber())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .active(customer.isActive())
                    .accountCount(customer.getAccounts().size())
                    .build())
                .build();
            
            log.info("Authentication successful for customer: {}", 
                    maskCustomerNumber(request.getCustomerNumber()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Authentication error for customer: {}", 
                     maskCustomerNumber(request.getCustomerNumber()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", 
                                        "Authentication service unavailable", 
                                        "/api/v1/auth/login/pin", 
                                        500));
        }
    }
    
    /**
     * Authenticates user with biometric data.
     */
    @PostMapping("/login/biometric")
    public ResponseEntity<?> authenticateWithBiometric(@Valid @RequestBody AuthenticationRequest request) {
        log.info("Biometric authentication request for customer: {}", 
                maskCustomerNumber(request.getCustomerNumber()));
        
        try {
            Optional<Customer> customerOpt = authenticationUseCase.authenticateWithBiometric(
                request.getCustomerNumber(), 
                request.getBiometricData()
            );
            
            if (customerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of("BIOMETRIC_AUTHENTICATION_FAILED", 
                                            "Biometric authentication failed", 
                                            "/api/v1/auth/login/biometric", 
                                            401));
            }
            
            Customer customer = customerOpt.get();
            
            // Verify device is registered
            if (!authenticationUseCase.isDeviceRegistered(customer.getId(), request.getDeviceId())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of("DEVICE_NOT_REGISTERED", 
                                            "Device not registered for this account", 
                                            "/api/v1/auth/login/biometric", 
                                            401));
            }
            
            String token = jwtTokenProvider.generateToken(customer.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(customer.getId());
            
            AuthenticationResponse response = AuthenticationResponse.builder()
                .success(true)
                .message("Biometric authentication successful")
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .customer(AuthenticationResponse.CustomerInfo.builder()
                    .customerId(customer.getId())
                    .customerNumber(customer.getCustomerNumber())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .active(customer.isActive())
                    .accountCount(customer.getAccounts().size())
                    .build())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Biometric authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", 
                                        "Authentication service unavailable", 
                                        "/api/v1/auth/login/biometric", 
                                        500));
        }
    }
    
    /**
     * Registers a new device for a customer.
     */
    @PostMapping("/register-device")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        log.info("Device registration request for customer: {}", request.getCustomerId());
        
        try {
            authenticationUseCase.registerDevice(
                request.getCustomerId(),
                request.getDeviceId(),
                request.getDeviceName(),
                request.getPublicKey()
            );
            
            return ResponseEntity.ok()
                .body("{\"success\": true, \"message\": \"Device registered successfully\"}");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("INVALID_REQUEST", 
                                        e.getMessage(), 
                                        "/api/v1/auth/register-device", 
                                        400));
        } catch (Exception e) {
            log.error("Device registration error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", 
                                        "Device registration failed", 
                                        "/api/v1/auth/register-device", 
                                        500));
        }
    }
    
    /**
     * Checks if a device is registered for a customer.
     */
    @GetMapping("/device-status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> checkDeviceStatus(
            @RequestParam String customerId,
            @RequestParam String deviceId) {
        
        try {
            boolean isRegistered = authenticationUseCase.isDeviceRegistered(customerId, deviceId);
            
            return ResponseEntity.ok()
                .body("{\"registered\": " + isRegistered + ", \"deviceId\": \"" + deviceId + "\"}");
            
        } catch (Exception e) {
            log.error("Device status check error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", 
                                        "Device status check failed", 
                                        "/api/v1/auth/device-status", 
                                        500));
        }
    }
    
    /**
     * Refreshes an expired JWT token using a refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody String refreshToken) {
        log.debug("Token refresh request");
        
        try {
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of("INVALID_REFRESH_TOKEN", 
                                            "Refresh token is invalid or expired", 
                                            "/api/v1/auth/refresh", 
                                            401));
            }
            
            String customerId = jwtTokenProvider.getCustomerIdFromToken(refreshToken);
            String newToken = jwtTokenProvider.generateToken(customerId);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(customerId);
            
            AuthenticationResponse response = AuthenticationResponse.builder()
                .success(true)
                .message("Token refreshed successfully")
                .token(newToken)
                .refreshToken(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of("TOKEN_REFRESH_FAILED", 
                                        "Unable to refresh token", 
                                        "/api/v1/auth/refresh", 
                                        401));
        }
    }
    
    /**
     * Logs out a user by invalidating their token.
     */
    @PostMapping("/logout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> logout(@RequestParam String customerId) {
        log.info("Logout request for customer: {}", customerId);
        
        try {
            authenticationUseCase.logout(customerId);
            
            return ResponseEntity.ok()
                .body("{\"success\": true, \"message\": \"Logged out successfully\"}");
            
        } catch (Exception e) {
            log.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("LOGOUT_FAILED", 
                                        "Logout failed", 
                                        "/api/v1/auth/logout", 
                                        500));
        }
    }
    
    private String maskCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.length() <= 4) {
            return "****";
        }
        return "****" + customerNumber.substring(customerNumber.length() - 4);
    }
}