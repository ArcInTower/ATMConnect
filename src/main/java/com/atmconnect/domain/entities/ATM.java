package com.atmconnect.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ATM {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "atm_code", unique = true, nullable = false)
    private String atmCode;
    
    @Column(name = "location_name", nullable = false)
    private String locationName;
    
    @Column(name = "address", nullable = false)
    private String address;
    
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    
    @Column(name = "longitude", nullable = false)
    private Double longitude;
    
    @Column(name = "bluetooth_mac", unique = true, nullable = false)
    private String bluetoothMacAddress;
    
    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(name = "certificate", nullable = false, columnDefinition = "TEXT")
    private String certificate;
    
    @Column(name = "is_active", nullable = false)
    private boolean active;
    
    @Column(name = "is_online", nullable = false)
    private boolean online;
    
    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
    
    @Column(name = "cash_available", nullable = false)
    private boolean cashAvailable;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public void updateHeartbeat() {
        this.lastHeartbeat = LocalDateTime.now();
        this.online = true;
    }
    
    public boolean isAvailable() {
        return active && online && cashAvailable && isRecentlyActive();
    }
    
    private boolean isRecentlyActive() {
        return lastHeartbeat != null && 
               lastHeartbeat.isAfter(LocalDateTime.now().minusMinutes(5));
    }
}