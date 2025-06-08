package com.atmconnect.presentation.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring and load balancer probes.
 * 
 * Provides endpoints for:
 * - Basic health status
 * - Readiness checks
 * - Application info
 */
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    /**
     * Basic health check endpoint.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "ATMConnect");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Readiness check for Kubernetes/container orchestration.
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();
        
        try {
            // Check if application is ready to serve requests
            // In production, this would check database connectivity, 
            // external service dependencies, etc.
            
            boolean isReady = checkApplicationReadiness();
            
            if (isReady) {
                readiness.put("status", "READY");
                readiness.put("timestamp", LocalDateTime.now());
                return ResponseEntity.ok(readiness);
            } else {
                readiness.put("status", "NOT_READY");
                readiness.put("timestamp", LocalDateTime.now());
                return ResponseEntity.status(503).body(readiness);
            }
            
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            readiness.put("status", "NOT_READY");
            readiness.put("error", e.getMessage());
            readiness.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(503).body(readiness);
        }
    }
    
    /**
     * Liveness check for Kubernetes/container orchestration.
     */
    @GetMapping("/live")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(liveness);
    }
    
    /**
     * Application information endpoint.
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "ATMConnect");
        info.put("description", "Secure Banking Application with BLE Connectivity");
        info.put("version", "1.0.0");
        info.put("build-time", LocalDateTime.now());
        info.put("architecture", "Hexagonal/Clean Architecture");
        info.put("security", "AES-256-GCM, ECDH, JWT");
        info.put("bluetooth", "BLE 5.0+ with GATT services");
        
        Map<String, String> features = new HashMap<>();
        features.put("authentication", "Multi-factor (PIN + Biometric + Device)");
        features.put("transactions", "Withdrawal, Transfer, Balance Inquiry");
        features.put("connectivity", "Bluetooth Low Energy (ATM Peripheral)");
        features.put("security", "PCI DSS Compliant");
        
        info.put("features", features);
        
        return ResponseEntity.ok(info);
    }
    
    private boolean checkApplicationReadiness() {
        // In production, implement actual readiness checks:
        // - Database connectivity
        // - External service availability
        // - Cache connectivity
        // - Message queue connectivity
        // - Configuration validation
        
        // For now, return true (application is ready)
        return true;
    }
}