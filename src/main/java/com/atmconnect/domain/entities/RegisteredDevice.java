package com.atmconnect.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registered_devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "device_id", unique = true, nullable = false)
    private String deviceId;
    
    @Column(name = "device_name", nullable = false)
    private String deviceName;
    
    @Column(name = "device_model")
    private String deviceModel;
    
    @Column(name = "operating_system")
    private String operatingSystem;
    
    @Column(name = "device_fingerprint", nullable = false)
    private String deviceFingerprint;
    
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return lastUsedAt != null && lastUsedAt.isBefore(LocalDateTime.now().minusDays(90));
    }
    
    public void deactivate() {
        this.active = false;
    }
}