package com.atmconnect.application.usecases;

import com.atmconnect.domain.entities.Customer;
import com.atmconnect.domain.entities.RegisteredDevice;
import com.atmconnect.domain.ports.inbound.AuthenticationUseCase;
import com.atmconnect.domain.ports.outbound.CustomerRepository;
import com.atmconnect.infrastructure.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationUseCaseImpl implements AuthenticationUseCase {
    
    private final CustomerRepository customerRepository;
    private final CryptoService cryptoService;
    
    @Override
    @Transactional
    public Optional<Customer> authenticateWithPin(String customerNumber, String pin) {
        log.info("Attempting PIN authentication for customer: {}", customerNumber);
        
        Optional<Customer> customerOpt = customerRepository.findByCustomerNumber(customerNumber);
        
        if (customerOpt.isEmpty()) {
            log.warn("Customer not found: {}", customerNumber);
            return Optional.empty();
        }
        
        Customer customer = customerOpt.get();
        
        if (!customer.isActive()) {
            log.warn("Inactive customer attempted login: {}", customerNumber);
            return Optional.empty();
        }
        
        if (customer.isLocked()) {
            log.warn("Locked customer attempted login: {}", customerNumber);
            return Optional.empty();
        }
        
        try {
            if (customer.verifyPin(pin)) {
                log.info("PIN authentication successful for customer: {}", customerNumber);
                customerRepository.save(customer);
                return Optional.of(customer);
            } else {
                log.warn("Invalid PIN for customer: {}", customerNumber);
                customerRepository.save(customer);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error during PIN authentication", e);
            return Optional.empty();
        }
    }
    
    @Override
    @Transactional
    public Optional<Customer> authenticateWithBiometric(String customerNumber, String biometricData) {
        log.info("Attempting biometric authentication for customer: {}", customerNumber);
        
        Optional<Customer> customerOpt = customerRepository.findByCustomerNumber(customerNumber);
        
        if (customerOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Customer customer = customerOpt.get();
        
        if (!customer.isActive() || customer.isLocked()) {
            return Optional.empty();
        }
        
        if (!customer.isBiometricEnabled()) {
            log.warn("Biometric not enabled for customer: {}", customerNumber);
            return Optional.empty();
        }
        
        // In production, this would verify biometric data against stored template
        if (verifyBiometric(customer.getBiometricData(), biometricData)) {
            log.info("Biometric authentication successful for customer: {}", customerNumber);
            customer.setFailedAttempts(0);
            customerRepository.save(customer);
            return Optional.of(customer);
        }
        
        return Optional.empty();
    }
    
    @Override
    @Transactional
    public Optional<Customer> authenticateWithMultiFactor(String customerNumber, String pin, String deviceId) {
        log.info("Attempting multi-factor authentication for customer: {}", customerNumber);
        
        // First verify PIN
        Optional<Customer> customerOpt = authenticateWithPin(customerNumber, pin);
        
        if (customerOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Customer customer = customerOpt.get();
        
        // Then verify device
        if (!customer.isDeviceRegistered(deviceId)) {
            log.warn("Unregistered device {} for customer: {}", deviceId, customerNumber);
            return Optional.empty();
        }
        
        // Update last used time for device
        customer.getRegisteredDevices().stream()
            .filter(device -> device.getDeviceId().equals(deviceId))
            .findFirst()
            .ifPresent(device -> {
                device.updateLastUsed();
                log.info("Device {} verified and updated for customer: {}", deviceId, customerNumber);
            });
        
        customerRepository.save(customer);
        return Optional.of(customer);
    }
    
    @Override
    public void logout(String customerId) {
        log.info("Logging out customer: {}", customerId);
        // In production, this would invalidate tokens and clear session data
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isDeviceRegistered(String customerId, String deviceId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        return customerOpt.map(customer -> customer.isDeviceRegistered(deviceId)).orElse(false);
    }
    
    @Override
    @Transactional
    public void registerDevice(String customerId, String deviceId, String deviceName, String publicKey) {
        log.info("Registering device {} for customer: {}", deviceId, customerId);
        
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Generate device fingerprint
        String deviceFingerprint = cryptoService.computeHash(
            (deviceId + deviceName + publicKey).getBytes()
        );
        
        RegisteredDevice device = RegisteredDevice.builder()
            .deviceId(deviceId)
            .deviceName(deviceName)
            .deviceFingerprint(deviceFingerprint)
            .publicKey(publicKey)
            .build();
        
        customer.registerDevice(device);
        customerRepository.save(customer);
        
        log.info("Device {} successfully registered for customer: {}", deviceId, customerId);
    }
    
    private boolean verifyBiometric(String storedTemplate, String providedData) {
        // In production, this would use actual biometric verification
        // For demonstration, we'll use a simple hash comparison
        if (storedTemplate == null || providedData == null) {
            return false;
        }
        
        String providedHash = cryptoService.computeHash(providedData.getBytes());
        return storedTemplate.equals(providedHash);
    }
}