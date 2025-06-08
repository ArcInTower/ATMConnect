package com.atmconnect.domain.ports.inbound;

import com.atmconnect.domain.entities.Customer;
import java.util.Optional;

public interface AuthenticationUseCase {
    Optional<Customer> authenticateWithPin(String customerNumber, String pin);
    Optional<Customer> authenticateWithBiometric(String customerNumber, String biometricData);
    Optional<Customer> authenticateWithMultiFactor(String customerNumber, String pin, String deviceId);
    void logout(String customerId);
    boolean isDeviceRegistered(String customerId, String deviceId);
    void registerDevice(String customerId, String deviceId, String deviceName, String publicKey);
}