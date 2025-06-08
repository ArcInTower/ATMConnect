package com.atmconnect.application.usecases;

import com.atmconnect.domain.entities.Customer;
import com.atmconnect.domain.entities.RegisteredDevice;
import com.atmconnect.domain.ports.outbound.CustomerRepository;
import com.atmconnect.domain.valueobjects.Pin;
import com.atmconnect.infrastructure.security.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseImplTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CryptoService cryptoService;
    
    @InjectMocks
    private AuthenticationUseCaseImpl authenticationUseCase;
    
    private Customer customer;
    private Pin validPin;
    
    @BeforeEach
    void setUp() {
        validPin = new Pin("123456");
        
        customer = Customer.builder()
            .id("customer-1")
            .customerNumber("12345678")
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .phoneNumber("+1234567890")
            .pinHash(validPin.getHashedPin())
            .pinSalt(validPin.getSalt())
            .biometricEnabled(true)
            .biometricData("biometric-hash")
            .active(true)
            .failedAttempts(0)
            .registeredDevices(new HashSet<>())
            .build();
    }
    
    @Test
    @DisplayName("Should authenticate customer with valid PIN")
    void shouldAuthenticateCustomerWithValidPin() {
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        Optional<Customer> result = authenticationUseCase.authenticateWithPin("12345678", "123456");
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("customer-1");
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(customerRepository).save(customer);
    }
    
    @Test
    @DisplayName("Should fail authentication with invalid PIN")
    void shouldFailAuthenticationWithInvalidPin() {
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        Optional<Customer> result = authenticationUseCase.authenticateWithPin("12345678", "wrong-pin");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(customerRepository).save(customer);
    }
    
    @Test
    @DisplayName("Should fail authentication for non-existent customer")
    void shouldFailAuthenticationForNonExistentCustomer() {
        when(customerRepository.findByCustomerNumber("99999999"))
            .thenReturn(Optional.empty());
        
        Optional<Customer> result = authenticationUseCase.authenticateWithPin("99999999", "123456");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository).findByCustomerNumber("99999999");
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should fail authentication for inactive customer")
    void shouldFailAuthenticationForInactiveCustomer() {
        customer.setActive(false);
        
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        
        Optional<Customer> result = authenticationUseCase.authenticateWithPin("12345678", "123456");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should fail authentication for locked customer")
    void shouldFailAuthenticationForLockedCustomer() {
        customer.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        
        Optional<Customer> result = authenticationUseCase.authenticateWithPin("12345678", "123456");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should authenticate with biometric when enabled")
    void shouldAuthenticateWithBiometricWhenEnabled() {
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        when(cryptoService.computeHash(any(byte[].class)))
            .thenReturn("biometric-hash");
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        Optional<Customer> result = authenticationUseCase.authenticateWithBiometric("12345678", "biometric-data");
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("customer-1");
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(cryptoService).computeHash(any(byte[].class));
        verify(customerRepository).save(customer);
    }
    
    @Test
    @DisplayName("Should fail biometric authentication when disabled")
    void shouldFailBiometricAuthenticationWhenDisabled() {
        customer.setBiometricEnabled(false);
        
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        
        Optional<Customer> result = authenticationUseCase.authenticateWithBiometric("12345678", "biometric-data");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository).findByCustomerNumber("12345678");
        verify(cryptoService, never()).computeHash(any(byte[].class));
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should authenticate with multi-factor when device is registered")
    void shouldAuthenticateWithMultiFactorWhenDeviceIsRegistered() {
        RegisteredDevice device = RegisteredDevice.builder()
            .deviceId("device-123")
            .deviceName("iPhone")
            .active(true)
            .customer(customer)
            .build();
        
        Set<RegisteredDevice> devices = new HashSet<>();
        devices.add(device);
        customer.setRegisteredDevices(devices);
        
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        Optional<Customer> result = authenticationUseCase.authenticateWithMultiFactor("12345678", "123456", "device-123");
        
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("customer-1");
        
        verify(customerRepository, times(2)).findByCustomerNumber("12345678");
        verify(customerRepository, times(2)).save(customer);
    }
    
    @Test
    @DisplayName("Should fail multi-factor authentication with unregistered device")
    void shouldFailMultiFactorAuthenticationWithUnregisteredDevice() {
        when(customerRepository.findByCustomerNumber("12345678"))
            .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        Optional<Customer> result = authenticationUseCase.authenticateWithMultiFactor("12345678", "123456", "unknown-device");
        
        assertThat(result).isEmpty();
        
        verify(customerRepository, times(2)).findByCustomerNumber("12345678");
        verify(customerRepository).save(customer);
    }
    
    @Test
    @DisplayName("Should register device successfully")
    void shouldRegisterDeviceSuccessfully() {
        when(customerRepository.findById("customer-1"))
            .thenReturn(Optional.of(customer));
        when(cryptoService.computeHash(any(byte[].class)))
            .thenReturn("device-fingerprint");
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(customer);
        
        assertThatCode(() -> authenticationUseCase.registerDevice(
            "customer-1", "device-123", "iPhone", "public-key"))
            .doesNotThrowAnyException();
        
        verify(customerRepository).findById("customer-1");
        verify(cryptoService).computeHash(any(byte[].class));
        verify(customerRepository).save(customer);
    }
    
    @Test
    @DisplayName("Should check if device is registered")
    void shouldCheckIfDeviceIsRegistered() {
        RegisteredDevice device = RegisteredDevice.builder()
            .deviceId("device-123")
            .active(true)
            .build();
        
        Set<RegisteredDevice> devices = new HashSet<>();
        devices.add(device);
        customer.setRegisteredDevices(devices);
        
        when(customerRepository.findById("customer-1"))
            .thenReturn(Optional.of(customer));
        
        boolean isRegistered = authenticationUseCase.isDeviceRegistered("customer-1", "device-123");
        boolean isNotRegistered = authenticationUseCase.isDeviceRegistered("customer-1", "device-456");
        
        assertThat(isRegistered).isTrue();
        assertThat(isNotRegistered).isFalse();
        
        verify(customerRepository, times(2)).findById("customer-1");
    }
}