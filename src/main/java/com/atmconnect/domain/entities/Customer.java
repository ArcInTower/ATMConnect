package com.atmconnect.domain.entities;

import com.atmconnect.domain.valueobjects.Pin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "customer_number", unique = true, nullable = false)
    private String customerNumber;
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    @Column(name = "email", unique = true, nullable = false)
    private String email;
    
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;
    
    @Column(name = "pin_salt", nullable = false)
    private String pinSalt;
    
    @Column(name = "biometric_enabled", nullable = false)
    private boolean biometricEnabled;
    
    @Column(name = "biometric_data")
    private String biometricData;
    
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RegisteredDevice> registeredDevices = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public boolean verifyPin(String plainPin) {
        if (isLocked()) {
            throw new IllegalStateException("Account is locked");
        }
        
        Pin storedPin = Pin.fromHash(pinHash, pinSalt);
        boolean isValid = storedPin.verify(plainPin);
        
        if (!isValid) {
            incrementFailedAttempts();
        } else {
            resetFailedAttempts();
        }
        
        return isValid;
    }
    
    public void updatePin(Pin newPin) {
        this.pinHash = newPin.getHashedPin();
        this.pinSalt = newPin.getSalt();
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
    
    private void incrementFailedAttempts() {
        failedAttempts++;
        if (failedAttempts >= 3) {
            lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
    
    private void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }
    
    public void addAccount(Account account) {
        accounts.add(account);
        account.setCustomer(this);
    }
    
    public void registerDevice(RegisteredDevice device) {
        registeredDevices.add(device);
        device.setCustomer(this);
    }
    
    public boolean isDeviceRegistered(String deviceId) {
        return registeredDevices.stream()
            .anyMatch(device -> device.getDeviceId().equals(deviceId) && device.isActive());
    }
}