package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized BLE error handling and recovery mechanisms.
 * 
 * <p>This component provides sophisticated error handling for BLE operations
 * including automatic recovery, circuit breaker patterns, and comprehensive
 * error classification for banking-grade reliability.
 */
@Slf4j
@Component
public class BLEErrorHandler {
    
    // Error tracking
    private final Map<String, BLEErrorTracker> deviceErrorTrackers = new ConcurrentHashMap<>();
    private final Map<Integer, String> errorCodeMessages = new ConcurrentHashMap<>();
    
    // Circuit breaker parameters
    private static final int MAX_CONSECUTIVE_ERRORS = 3;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 30000; // 30 seconds
    private static final int ERROR_RATE_THRESHOLD = 5; // errors per minute
    
    public BLEErrorHandler() {
        initializeErrorCodeMessages();
    }
    
    /**
     * Handles a BLE error and determines appropriate recovery action.
     * 
     * @param deviceAddress device where error occurred
     * @param errorCode BLE error code
     * @param operation operation that failed
     * @param exception optional exception details
     * @return recovery action to take
     */
    public BLERecoveryAction handleError(String deviceAddress, int errorCode, 
                                       String operation, Throwable exception) {
        
        log.debug("Handling BLE error for device {}: code={}, operation={}", 
                 deviceAddress, errorCode, operation);
        
        // Get or create error tracker for device
        BLEErrorTracker tracker = deviceErrorTrackers.computeIfAbsent(
            deviceAddress, k -> new BLEErrorTracker(deviceAddress)
        );
        
        // Record the error
        BLEError error = new BLEError(errorCode, operation, exception, Instant.now());
        tracker.recordError(error);
        
        // Classify error severity
        BLEErrorSeverity severity = classifyError(errorCode);
        
        // Determine recovery action
        BLERecoveryAction action = determineRecoveryAction(tracker, error, severity);
        
        // Log the error and action
        logErrorAndAction(deviceAddress, error, severity, action);
        
        return action;
    }
    
    /**
     * Checks if a device is currently under circuit breaker protection.
     * 
     * @param deviceAddress device to check
     * @return true if device is blocked by circuit breaker
     */
    public boolean isDeviceBlocked(String deviceAddress) {
        BLEErrorTracker tracker = deviceErrorTrackers.get(deviceAddress);
        if (tracker == null) {
            return false;
        }
        
        return tracker.isCircuitBreakerOpen();
    }
    
    /**
     * Resets error tracking for a device (called on successful operation).
     * 
     * @param deviceAddress device to reset
     */
    public void resetErrorTracking(String deviceAddress) {
        BLEErrorTracker tracker = deviceErrorTrackers.get(deviceAddress);
        if (tracker != null) {
            tracker.reset();
            log.debug("Error tracking reset for device: {}", deviceAddress);
        }
    }
    
    /**
     * Gets error statistics for a device.
     * 
     * @param deviceAddress device to check
     * @return error statistics or null if no errors recorded
     */
    public BLEErrorStatistics getErrorStatistics(String deviceAddress) {
        BLEErrorTracker tracker = deviceErrorTrackers.get(deviceAddress);
        return tracker != null ? tracker.getStatistics() : null;
    }
    
    /**
     * Creates a user-friendly error message for display.
     * 
     * @param errorCode BLE error code
     * @return user-friendly error message
     */
    public String getUserFriendlyMessage(int errorCode) {
        BLEErrorSeverity severity = classifyError(errorCode);
        
        switch (severity) {
            case CRITICAL:
                return "Unable to connect to ATM. Please try a different ATM or contact support.";
            case HIGH:
                return "Connection to ATM lost. Please move closer and try again.";
            case MEDIUM:
                return "Transaction temporarily unavailable. Please wait and try again.";
            case LOW:
                return "Minor connectivity issue. Retrying automatically.";
            default:
                return "An unexpected error occurred. Please try again.";
        }
    }
    
    /**
     * Checks if an error is recoverable through retry.
     * 
     * @param errorCode BLE error code
     * @return true if error is potentially recoverable
     */
    public boolean isRecoverable(int errorCode) {
        switch (errorCode) {
            case BLEConstants.ERROR_CONNECTION_FAILED:
            case BLEConstants.ERROR_RSSI_TOO_LOW:
            case BLEConstants.ERROR_RANGE_EXCEEDED:
            case BLEConstants.ERROR_TRANSACTION_TIMEOUT:
                return true;
            case BLEConstants.ERROR_CERTIFICATE_INVALID:
            case BLEConstants.ERROR_AUTHENTICATION_FAILED:
            case BLEConstants.ERROR_ENCRYPTION_FAILED:
            case BLEConstants.ERROR_PAIRING_FAILED:
                return false;
            default:
                return true; // Default to recoverable for unknown errors
        }
    }
    
    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================
    
    /**
     * Initializes error code to message mappings.
     */
    private void initializeErrorCodeMessages() {
        errorCodeMessages.put(BLEConstants.ERROR_CONNECTION_FAILED, 
            "Failed to establish BLE connection");
        errorCodeMessages.put(BLEConstants.ERROR_AUTHENTICATION_FAILED, 
            "Authentication with ATM failed");
        errorCodeMessages.put(BLEConstants.ERROR_CERTIFICATE_INVALID, 
            "ATM certificate validation failed");
        errorCodeMessages.put(BLEConstants.ERROR_TRANSACTION_TIMEOUT, 
            "Transaction timed out");
        errorCodeMessages.put(BLEConstants.ERROR_GATT_SERVICE_NOT_FOUND, 
            "ATM services not available");
        errorCodeMessages.put(BLEConstants.ERROR_CHARACTERISTIC_NOT_FOUND, 
            "Required ATM feature not available");
        errorCodeMessages.put(BLEConstants.ERROR_ENCRYPTION_FAILED, 
            "Secure connection could not be established");
        errorCodeMessages.put(BLEConstants.ERROR_RSSI_TOO_LOW, 
            "Signal strength too low for secure connection");
        errorCodeMessages.put(BLEConstants.ERROR_RANGE_EXCEEDED, 
            "Device too far from ATM");
        errorCodeMessages.put(BLEConstants.ERROR_ADVERTISING_FAILED, 
            "ATM advertising failed");
        errorCodeMessages.put(BLEConstants.ERROR_PAIRING_FAILED, 
            "BLE pairing with ATM failed");
    }
    
    /**
     * Classifies error severity based on error code.
     */
    private BLEErrorSeverity classifyError(int errorCode) {
        switch (errorCode) {
            case BLEConstants.ERROR_CERTIFICATE_INVALID:
            case BLEConstants.ERROR_ENCRYPTION_FAILED:
            case BLEConstants.ERROR_PAIRING_FAILED:
                return BLEErrorSeverity.CRITICAL;
                
            case BLEConstants.ERROR_AUTHENTICATION_FAILED:
            case BLEConstants.ERROR_GATT_SERVICE_NOT_FOUND:
            case BLEConstants.ERROR_CHARACTERISTIC_NOT_FOUND:
                return BLEErrorSeverity.HIGH;
                
            case BLEConstants.ERROR_CONNECTION_FAILED:
            case BLEConstants.ERROR_TRANSACTION_TIMEOUT:
            case BLEConstants.ERROR_ADVERTISING_FAILED:
                return BLEErrorSeverity.MEDIUM;
                
            case BLEConstants.ERROR_RSSI_TOO_LOW:
            case BLEConstants.ERROR_RANGE_EXCEEDED:
                return BLEErrorSeverity.LOW;
                
            default:\n                return BLEErrorSeverity.MEDIUM; // Default severity for unknown errors
        }
    }
    
    /**
     * Determines the appropriate recovery action based on error history and severity.
     */
    private BLERecoveryAction determineRecoveryAction(BLEErrorTracker tracker, 
                                                     BLEError error, 
                                                     BLEErrorSeverity severity) {
        
        // Check circuit breaker status
        if (tracker.shouldOpenCircuitBreaker()) {
            tracker.openCircuitBreaker();
            return BLERecoveryAction.CIRCUIT_BREAKER_OPEN;
        }
        
        // Critical errors require immediate disconnect
        if (severity == BLEErrorSeverity.CRITICAL) {
            return BLERecoveryAction.DISCONNECT_AND_FAIL;
        }
        
        // High severity errors with multiple failures
        if (severity == BLEErrorSeverity.HIGH && tracker.getConsecutiveErrors() >= 2) {
            return BLERecoveryAction.DISCONNECT_AND_RETRY;
        }
        
        // Check if error is recoverable
        if (!isRecoverable(error.getErrorCode())) {
            return BLERecoveryAction.DISCONNECT_AND_FAIL;
        }
        
        // Determine retry strategy based on error type and count
        int consecutiveErrors = tracker.getConsecutiveErrors();
        
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            return BLERecoveryAction.DISCONNECT_AND_RETRY;
        } else if (consecutiveErrors >= 2) {
            return BLERecoveryAction.RETRY_WITH_BACKOFF;
        } else {
            return BLERecoveryAction.RETRY_IMMEDIATE;
        }
    }
    
    /**
     * Logs error details and recovery action.
     */
    private void logErrorAndAction(String deviceAddress, BLEError error, 
                                 BLEErrorSeverity severity, BLERecoveryAction action) {
        
        String errorMessage = errorCodeMessages.getOrDefault(
            error.getErrorCode(), 
            "Unknown BLE error: " + error.getErrorCode()
        );
        
        log.warn("BLE error on device {}: {} (severity: {}, action: {}, operation: {})",
                deviceAddress, errorMessage, severity, action, error.getOperation());
        
        if (error.getException() != null) {
            log.debug("Error details for device {}: ", deviceAddress, error.getException());
        }
    }
    
    // ============================================================================
    // INNER CLASSES AND ENUMS
    // ============================================================================
    
    /**
     * BLE error severity levels.
     */
    public enum BLEErrorSeverity {
        LOW,      // Recoverable errors that don't affect functionality
        MEDIUM,   // Errors that may affect current operation but are recoverable
        HIGH,     // Serious errors that require intervention
        CRITICAL  // Security or safety critical errors requiring immediate action
    }
    
    /**
     * Recovery actions that can be taken for BLE errors.
     */
    public enum BLERecoveryAction {
        RETRY_IMMEDIATE,        // Retry the operation immediately
        RETRY_WITH_BACKOFF,     // Retry with exponential backoff
        DISCONNECT_AND_RETRY,   // Disconnect and attempt to reconnect
        DISCONNECT_AND_FAIL,    // Disconnect and report failure
        CIRCUIT_BREAKER_OPEN,   // Temporarily block all operations
        IGNORE                  // Ignore the error (for non-critical operations)
    }
    
    /**
     * Represents a single BLE error occurrence.
     */
    public static class BLEError {
        private final int errorCode;
        private final String operation;
        private final Throwable exception;
        private final Instant timestamp;
        
        public BLEError(int errorCode, String operation, Throwable exception, Instant timestamp) {
            this.errorCode = errorCode;
            this.operation = operation;
            this.exception = exception;
            this.timestamp = timestamp;
        }
        
        public int getErrorCode() { return errorCode; }
        public String getOperation() { return operation; }
        public Throwable getException() { return exception; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Tracks error history and circuit breaker state for a specific device.
     */
    private static class BLEErrorTracker {
        private final String deviceAddress;
        private final AtomicInteger totalErrors = new AtomicInteger(0);
        private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
        private volatile Instant lastErrorTime;
        private volatile Instant circuitBreakerOpenTime;
        private volatile boolean circuitBreakerOpen = false;
        
        public BLEErrorTracker(String deviceAddress) {
            this.deviceAddress = deviceAddress;
        }
        
        public void recordError(BLEError error) {
            totalErrors.incrementAndGet();
            consecutiveErrors.incrementAndGet();
            lastErrorTime = error.getTimestamp();
        }
        
        public void reset() {
            consecutiveErrors.set(0);
            circuitBreakerOpen = false;
            circuitBreakerOpenTime = null;
        }
        
        public boolean shouldOpenCircuitBreaker() {
            return consecutiveErrors.get() >= MAX_CONSECUTIVE_ERRORS;
        }
        
        public void openCircuitBreaker() {
            circuitBreakerOpen = true;
            circuitBreakerOpenTime = Instant.now();
        }
        
        public boolean isCircuitBreakerOpen() {
            if (!circuitBreakerOpen) {
                return false;
            }
            
            // Check if circuit breaker timeout has passed
            if (circuitBreakerOpenTime != null) {
                long elapsedMs = Instant.now().toEpochMilli() - circuitBreakerOpenTime.toEpochMilli();
                if (elapsedMs > CIRCUIT_BREAKER_TIMEOUT_MS) {
                    // Reset circuit breaker
                    circuitBreakerOpen = false;
                    circuitBreakerOpenTime = null;
                    consecutiveErrors.set(0);
                    return false;
                }
            }
            
            return true;
        }
        
        public int getConsecutiveErrors() {
            return consecutiveErrors.get();
        }
        
        public BLEErrorStatistics getStatistics() {
            return new BLEErrorStatistics(
                deviceAddress,
                totalErrors.get(),
                consecutiveErrors.get(),
                lastErrorTime,
                circuitBreakerOpen
            );
        }
    }
    
    /**
     * Error statistics for a specific device.
     */
    public static class BLEErrorStatistics {
        private final String deviceAddress;
        private final int totalErrors;
        private final int consecutiveErrors;
        private final Instant lastErrorTime;
        private final boolean circuitBreakerOpen;
        
        public BLEErrorStatistics(String deviceAddress, int totalErrors, 
                                int consecutiveErrors, Instant lastErrorTime, 
                                boolean circuitBreakerOpen) {
            this.deviceAddress = deviceAddress;
            this.totalErrors = totalErrors;
            this.consecutiveErrors = consecutiveErrors;
            this.lastErrorTime = lastErrorTime;
            this.circuitBreakerOpen = circuitBreakerOpen;
        }
        
        public String getDeviceAddress() { return deviceAddress; }
        public int getTotalErrors() { return totalErrors; }
        public int getConsecutiveErrors() { return consecutiveErrors; }
        public Instant getLastErrorTime() { return lastErrorTime; }
        public boolean isCircuitBreakerOpen() { return circuitBreakerOpen; }
        
        @Override
        public String toString() {
            return String.format("BLEErrorStatistics{device='%s', total=%d, consecutive=%d, " +
                               "lastError=%s, circuitBreaker=%s}",
                               deviceAddress, totalErrors, consecutiveErrors, 
                               lastErrorTime, circuitBreakerOpen);
        }
    }
}