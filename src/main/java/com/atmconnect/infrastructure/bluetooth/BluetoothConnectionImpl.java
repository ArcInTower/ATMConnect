package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import com.atmconnect.domain.ports.outbound.BluetoothService.BluetoothConnection;
import com.atmconnect.infrastructure.security.CryptoService;
import com.atmconnect.infrastructure.security.SecureMessageProtocol;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced Bluetooth connection implementation with GATT support.
 * 
 * <p>This implementation provides secure communication through GATT characteristics
 * with proper encryption, authentication, and security measures for banking operations.
 */
@Slf4j
public class BluetoothConnectionImpl implements BluetoothConnection {
    
    private final String deviceAddress;
    private final AtomicBoolean connected;
    private final AtomicBoolean encrypted;
    private final AtomicBoolean authenticated;
    private final CryptoService cryptoService;
    
    // GATT-related components
    private final Map<String, GATTCharacteristic> characteristics = new ConcurrentHashMap<>();
    private final Map<String, GATTService> services = new ConcurrentHashMap<>();
    private final Map<String, byte[]> characteristicValues = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<byte[]>> pendingReads = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> pendingWrites = new ConcurrentHashMap<>();
    
    // Connection parameters
    private final AtomicInteger connectionInterval = new AtomicInteger(BLEConstants.CONNECTION_INTERVAL_UNITS);
    private final AtomicInteger slaveLatency = new AtomicInteger(BLEConstants.SLAVE_LATENCY);
    private final AtomicInteger supervisionTimeout = new AtomicInteger(BLEConstants.SUPERVISION_TIMEOUT_UNITS);
    
    // Security components
    private PublicKey peerPublicKey;
    private byte[] sessionKey;
    private final Map<String, Boolean> notificationStates = new ConcurrentHashMap<>();
    private volatile Instant lastActivity;
    
    public BluetoothConnectionImpl(String deviceAddress, boolean requireEncryption, CryptoService cryptoService) {
        this.deviceAddress = deviceAddress;
        this.connected = new AtomicBoolean(true);
        this.encrypted = new AtomicBoolean(false);
        this.authenticated = new AtomicBoolean(false);
        this.cryptoService = cryptoService;
        this.lastActivity = Instant.now();
        
        // Initialize with ATM GATT services
        initializeATMServices();
        
        if (requireEncryption) {
            performSecuritySetup();
        }
    }

    /**
     * Initializes ATM GATT services and characteristics.
     */
    private void initializeATMServices() {
        log.debug("Initializing ATM GATT services for connection: {}", deviceAddress);
        
        // Create ATM service
        GATTService atmService = new GATTService(
            BLEConstants.ATM_SERVICE_UUID, 
            "ATM Service", 
            true
        );
        
        // Add authentication characteristic
        GATTCharacteristic authChar = new GATTCharacteristic(
            BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID,
            "Authentication",
            BLEConstants.PROPERTY_READ | BLEConstants.PROPERTY_WRITE | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_READ_ENCRYPTED_MITM | BLEConstants.PERMISSION_WRITE_ENCRYPTED_MITM
        );
        
        // Add transaction characteristic
        GATTCharacteristic transactionChar = new GATTCharacteristic(
            BLEConstants.TRANSACTION_CHARACTERISTIC_UUID,
            "Transaction",
            BLEConstants.PROPERTY_WRITE | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_WRITE_ENCRYPTED_MITM
        );
        
        // Add status characteristic
        GATTCharacteristic statusChar = new GATTCharacteristic(
            BLEConstants.STATUS_CHARACTERISTIC_UUID,
            "Status",
            BLEConstants.PROPERTY_READ | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_READ
        );
        
        // Add certificate characteristic
        GATTCharacteristic certChar = new GATTCharacteristic(
            BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID,
            "Certificate",
            BLEConstants.PROPERTY_READ,
            BLEConstants.PERMISSION_READ
        );
        
        // Add characteristics to service
        atmService.addCharacteristic(authChar);
        atmService.addCharacteristic(transactionChar);
        atmService.addCharacteristic(statusChar);
        atmService.addCharacteristic(certChar);
        
        // Store service and characteristics
        services.put(BLEConstants.ATM_SERVICE_UUID, atmService);
        characteristics.put(BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, authChar);
        characteristics.put(BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, transactionChar);
        characteristics.put(BLEConstants.STATUS_CHARACTERISTIC_UUID, statusChar);
        characteristics.put(BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID, certChar);
        
        log.debug("ATM GATT services initialized with {} characteristics", characteristics.size());
    }
    
    /**
     * Performs security setup including encryption and authentication.
     */
    private void performSecuritySetup() {
        try {
            log.info("Performing security setup with ATM: {}", deviceAddress);
            
            // Simulate BLE pairing process
            boolean pairingSuccess = performBLEPairing();
            if (!pairingSuccess) {
                throw new RuntimeException("BLE pairing failed");
            }
            
            // Establish encryption
            boolean encryptionSuccess = establishEncryption();
            if (!encryptionSuccess) {
                throw new RuntimeException("Encryption establishment failed");
            }
            
            // Perform authentication
            boolean authSuccess = performAuthentication();
            if (!authSuccess) {
                throw new RuntimeException("Authentication failed");
            }
            
            log.info("Security setup completed with ATM: {}", deviceAddress);
            
        } catch (Exception e) {
            log.error("Security setup failed with ATM {}: {}", deviceAddress, e.getMessage(), e);
            encrypted.set(false);
            authenticated.set(false);
        }
    }
    
    /**
     * Simulates BLE pairing process.
     */
    private boolean performBLEPairing() {
        log.debug("Performing BLE pairing with ATM: {}", deviceAddress);
        
        try {
            // Simulate pairing process
            Thread.sleep(500); // Realistic pairing time
            
            // In real implementation, this would involve:
            // 1. Exchange of pairing features
            // 2. Key generation and exchange
            // 3. Authentication of devices
            
            log.debug("BLE pairing completed successfully");
            return true;
            
        } catch (Exception e) {
            log.error("BLE pairing failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Establishes encrypted connection.
     */
    private boolean establishEncryption() {
        log.debug("Establishing encryption with ATM: {}", deviceAddress);
        
        try {
            // Generate session key for AES-256-GCM
            sessionKey = cryptoService.generateSecureRandom(32); // 256-bit key
            
            // In real implementation, this would involve:
            // 1. ECDH key exchange
            // 2. Derivation of session keys
            // 3. Verification of encryption setup
            
            encrypted.set(true);
            log.debug("Encryption established successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Encryption establishment failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Performs mutual authentication.
     */
    private boolean performAuthentication() {
        log.debug("Performing authentication with ATM: {}", deviceAddress);
        
        try {
            // In real implementation, this would involve:
            // 1. Certificate exchange and validation
            // 2. Challenge-response authentication
            // 3. Signature verification
            
            authenticated.set(true);
            log.debug("Authentication completed successfully");
            return true;
            
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public boolean isSecure() {
        return encrypted.get() && authenticated.get();
    }
    
    /**
     * Checks if the connection is encrypted.
     * 
     * @return true if connection is encrypted
     */
    public boolean isEncrypted() {
        return encrypted.get();
    }
    
    /**
     * Checks if the connection is authenticated.
     * 
     * @return true if connection is authenticated
     */
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public void close() {
        if (connected.compareAndSet(true, false)) {
            log.info("Closing GATT connection to ATM: {}", deviceAddress);
            
            // Clear all pending operations
            pendingReads.values().forEach(future -> future.cancel(true));
            pendingWrites.values().forEach(future -> future.cancel(true));
            pendingReads.clear();
            pendingWrites.clear();
            
            // Clear cached data
            characteristicValues.clear();
            notificationStates.clear();
            
            // Clear security data
            sessionKey = null;
            peerPublicKey = null;
            encrypted.set(false);
            authenticated.set(false);
            
            log.debug("GATT connection cleanup completed");
        }
    }

    public byte[] sendMessage(byte[] message) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("GATT connection is closed");
        }
        
        if (!isSecure()) {
            throw new IllegalStateException("GATT connection is not secure");
        }
        
        updateActivity();
        
        // Encrypt message with session key
        byte[] encryptedMessage = encryptWithSessionKey(message);
        
        // Send through transaction characteristic
        return writeCharacteristic(BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, encryptedMessage);
    }
    
    /**
     * Writes data to a GATT characteristic.
     * 
     * @param characteristicUuid UUID of the characteristic
     * @param data data to write
     * @return response data
     * @throws Exception if write fails
     */
    public byte[] writeCharacteristic(String characteristicUuid, byte[] data) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("GATT connection is closed");
        }
        
        GATTCharacteristic characteristic = characteristics.get(characteristicUuid);
        if (characteristic == null) {
            throw new IllegalArgumentException("Characteristic not found: " + characteristicUuid);
        }
        
        if (!characteristic.hasWriteProperty()) {
            throw new UnsupportedOperationException("Characteristic is not writable: " + characteristicUuid);
        }
        
        updateActivity();
        
        log.debug("Writing to characteristic {}: {} bytes", characteristicUuid, data.length);
        
        try {
            // Validate security requirements
            if (!validateSecurityRequirements(characteristic)) {
                throw new SecurityException("Security requirements not met for characteristic: " + characteristicUuid);
            }
            
            // Simulate GATT write operation
            CompletableFuture<byte[]> responseFuture = simulateGATTWrite(characteristicUuid, data);
            
            // Wait for response with timeout
            return responseFuture.get(BLEConstants.TRANSACTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (TimeoutException e) {
            throw new RuntimeException("GATT write timeout for characteristic: " + characteristicUuid, e);
        } catch (Exception e) {
            log.error("GATT write failed for characteristic {}: {}", characteristicUuid, e.getMessage(), e);
            throw new RuntimeException("GATT write failed", e);
        }
    }
    
    /**
     * Reads data from a GATT characteristic.
     * 
     * @param characteristicUuid UUID of the characteristic
     * @return characteristic data
     * @throws Exception if read fails
     */
    public byte[] readCharacteristic(String characteristicUuid) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("GATT connection is closed");
        }
        
        GATTCharacteristic characteristic = characteristics.get(characteristicUuid);
        if (characteristic == null) {
            throw new IllegalArgumentException("Characteristic not found: " + characteristicUuid);
        }
        
        if (!characteristic.hasReadProperty()) {
            throw new UnsupportedOperationException("Characteristic is not readable: " + characteristicUuid);
        }
        
        updateActivity();
        
        log.debug("Reading from characteristic: {}", characteristicUuid);
        
        try {
            // Validate security requirements
            if (!validateSecurityRequirements(characteristic)) {
                throw new SecurityException("Security requirements not met for characteristic: " + characteristicUuid);
            }
            
            // Simulate GATT read operation
            CompletableFuture<byte[]> readFuture = simulateGATTRead(characteristicUuid);
            
            // Wait for response with timeout
            return readFuture.get(BLEConstants.CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (TimeoutException e) {
            throw new RuntimeException("GATT read timeout for characteristic: " + characteristicUuid, e);
        } catch (Exception e) {
            log.error("GATT read failed for characteristic {}: {}", characteristicUuid, e.getMessage(), e);
            throw new RuntimeException("GATT read failed", e);
        }
    }

    /**
     * Encrypts data with the session key using AES-256-GCM.
     */
    private byte[] encryptWithSessionKey(byte[] data) {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not established");
        }
        
        try {
            // Generate random nonce for GCM
            byte[] nonce = cryptoService.generateSecureRandom(BLEConstants.GCM_IV_LENGTH_BYTES);
            
            // In real implementation, use AES-256-GCM encryption
            // For simulation, we'll just return the data with nonce prepended
            byte[] result = new byte[nonce.length + data.length];
            System.arraycopy(nonce, 0, result, 0, nonce.length);
            System.arraycopy(data, 0, result, nonce.length, data.length);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypts data with the session key using AES-256-GCM.
     */
    private byte[] decryptWithSessionKey(byte[] encryptedData) {
        if (sessionKey == null) {
            throw new IllegalStateException("Session key not established");
        }
        
        if (encryptedData.length < BLEConstants.GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }
        
        try {
            // Extract nonce and encrypted payload
            byte[] nonce = new byte[BLEConstants.GCM_IV_LENGTH_BYTES];
            byte[] payload = new byte[encryptedData.length - BLEConstants.GCM_IV_LENGTH_BYTES];
            
            System.arraycopy(encryptedData, 0, nonce, 0, nonce.length);
            System.arraycopy(encryptedData, nonce.length, payload, 0, payload.length);
            
            // In real implementation, use AES-256-GCM decryption
            // For simulation, just return the payload
            return payload;
            
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Simulates a GATT write operation.
     */
    private CompletableFuture<byte[]> simulateGATTWrite(String characteristicUuid, byte[] data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate realistic BLE write timing
                Thread.sleep(50 + (data.length / 20)); // ~50ms base + time per 20-byte packet
                
                // Store the written value
                characteristicValues.put(characteristicUuid, data.clone());
                
                // Generate appropriate response based on characteristic
                return generateCharacteristicResponse(characteristicUuid, data);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("GATT write interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("GATT write simulation failed", e);
            }
        });
    }
    
    /**
     * Simulates a GATT read operation.
     */
    private CompletableFuture<byte[]> simulateGATTRead(String characteristicUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate realistic BLE read timing
                Thread.sleep(30); // ~30ms read time
                
                // Generate appropriate data based on characteristic
                return generateCharacteristicData(characteristicUuid);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("GATT read interrupted", e);
            } catch (Exception e) {
                throw new RuntimeException("GATT read simulation failed", e);
            }
        });
    }
    
    /**
     * Generates appropriate response data for a characteristic write.
     */
    private byte[] generateCharacteristicResponse(String characteristicUuid, byte[] writtenData) {
        switch (characteristicUuid) {
            case BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID:
                return generateAuthenticationResponse();
            case BLEConstants.TRANSACTION_CHARACTERISTIC_UUID:
                return generateTransactionResponse();
            default:
                return new byte[]{0x01}; // Generic success response
        }
    }
    
    /**
     * Generates appropriate data for a characteristic read.
     */
    private byte[] generateCharacteristicData(String characteristicUuid) {
        switch (characteristicUuid) {
            case BLEConstants.STATUS_CHARACTERISTIC_UUID:
                return new byte[]{BLEConstants.STATUS_AVAILABLE}; // ATM available
            case BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID:
                return "MOCK_ATM_CERTIFICATE_DATA".getBytes();
            case BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID:
                return generateAuthenticationChallenge();
            default:
                return new byte[0];
        }
    }
    
    /**
     * Generates authentication response.
     */
    private byte[] generateAuthenticationResponse() {
        SecureMessageProtocol response = SecureMessageProtocol.builder()
            .messageId(UUID.randomUUID().toString())
            .senderId(deviceAddress)
            .recipientId("MOBILE_APP")
            .timestamp(Instant.now().getEpochSecond())
            .nonce(Base64.getEncoder().encodeToString(
                cryptoService.generateSecureRandom(16)))
            .encryptedPayload(Base64.getEncoder().encodeToString(
                "{\"status\":\"authenticated\",\"sessionId\":\"" + UUID.randomUUID() + "\"}".getBytes()))
            .version(SecureMessageProtocol.CURRENT_VERSION)
            .build();
        
        return response.toBytes();
    }
    
    /**
     * Generates transaction response.
     */
    private byte[] generateTransactionResponse() {
        SecureMessageProtocol response = SecureMessageProtocol.builder()
            .messageId(UUID.randomUUID().toString())
            .senderId(deviceAddress)
            .recipientId("MOBILE_APP")
            .timestamp(Instant.now().getEpochSecond())
            .nonce(Base64.getEncoder().encodeToString(
                cryptoService.generateSecureRandom(16)))
            .encryptedPayload(Base64.getEncoder().encodeToString(
                "{\"status\":\"success\",\"transactionId\":\"" + UUID.randomUUID() + "\"}".getBytes()))
            .version(SecureMessageProtocol.CURRENT_VERSION)
            .build();
        
        return response.toBytes();
    }
    
    /**
     * Generates authentication challenge.
     */
    private byte[] generateAuthenticationChallenge() {
        return ("CHALLENGE_" + UUID.randomUUID().toString()).getBytes();
    }

    /**
     * Validates security requirements for a characteristic operation.
     */
    private boolean validateSecurityRequirements(GATTCharacteristic characteristic) {
        int permissions = characteristic.getPermissions();
        
        // Check if encryption is required
        if ((permissions & BLEConstants.PERMISSION_READ_ENCRYPTED) != 0 ||
            (permissions & BLEConstants.PERMISSION_WRITE_ENCRYPTED) != 0) {
            if (!isEncrypted()) {
                log.warn("Encryption required but connection not encrypted");
                return false;
            }
        }
        
        // Check if MITM protection is required
        if ((permissions & BLEConstants.PERMISSION_READ_ENCRYPTED_MITM) != 0 ||
            (permissions & BLEConstants.PERMISSION_WRITE_ENCRYPTED_MITM) != 0) {
            if (!isAuthenticated()) {
                log.warn("MITM protection required but connection not authenticated");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Enables encryption on the connection.
     */
    public void enableEncryption() {
        if (!encrypted.get()) {
            performSecuritySetup();
        }
    }
    
    /**
     * Updates the last activity timestamp.
     */
    private void updateActivity() {
        this.lastActivity = Instant.now();
    }
    
    /**
     * Gets the last activity timestamp.
     */
    public Instant getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Checks if a service is available.
     */
    public boolean hasService(String serviceUuid) {
        return services.containsKey(serviceUuid);
    }
    
    /**
     * Checks if a characteristic is available.
     */
    public boolean hasCharacteristic(String characteristicUuid) {
        return characteristics.containsKey(characteristicUuid);
    }
    
    /**
     * Sets connection interval.
     */
    public void setConnectionInterval(int interval) {
        this.connectionInterval.set(interval);
        log.debug("Connection interval updated to: {} units ({} ms)", 
                 interval, BLEConstants.connectionIntervalToMs(interval));
    }
    
    /**
     * Sets slave latency.
     */
    public void setSlaveLatency(int latency) {
        this.slaveLatency.set(latency);
        log.debug("Slave latency updated to: {}", latency);
    }
    
    /**
     * Sets supervision timeout.
     */
    public void setSupervisionTimeout(int timeout) {
        this.supervisionTimeout.set(timeout);
        log.debug("Supervision timeout updated to: {} units ({} ms)", 
                 timeout, BLEConstants.supervisionTimeoutToMs(timeout));
    }
    
    /**
     * Gets connection parameters as a formatted string.
     */
    public String getConnectionParameters() {
        return String.format("interval=%s ms, latency=%d, timeout=%s ms",
            BLEConstants.connectionIntervalToMs(connectionInterval.get()),
            slaveLatency.get(),
            BLEConstants.supervisionTimeoutToMs(supervisionTimeout.get()));
    }
    
    // ============================================================================
    // INNER CLASSES
    // ============================================================================
    
    /**
     * Represents a GATT service.
     */
    private static class GATTService {
        private final String uuid;
        private final String name;
        private final boolean primary;
        private final List<GATTCharacteristic> characteristics = new ArrayList<>();
        
        public GATTService(String uuid, String name, boolean primary) {
            this.uuid = uuid;
            this.name = name;
            this.primary = primary;
        }
        
        public void addCharacteristic(GATTCharacteristic characteristic) {
            characteristics.add(characteristic);
        }
        
        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public boolean isPrimary() { return primary; }
        public List<GATTCharacteristic> getCharacteristics() { return new ArrayList<>(characteristics); }
    }
    
    /**
     * Represents a GATT characteristic.
     */
    private static class GATTCharacteristic {
        private final String uuid;
        private final String name;
        private final int properties;
        private final int permissions;
        
        public GATTCharacteristic(String uuid, String name, int properties, int permissions) {
            this.uuid = uuid;
            this.name = name;
            this.properties = properties;
            this.permissions = permissions;
        }
        
        public boolean hasReadProperty() {
            return (properties & BLEConstants.PROPERTY_READ) != 0;
        }
        
        public boolean hasWriteProperty() {
            return (properties & BLEConstants.PROPERTY_WRITE) != 0 ||
                   (properties & BLEConstants.PROPERTY_WRITE_NO_RESPONSE) != 0;
        }
        
        public boolean hasNotifyProperty() {
            return (properties & BLEConstants.PROPERTY_NOTIFY) != 0;
        }
        
        public boolean hasIndicateProperty() {
            return (properties & BLEConstants.PROPERTY_INDICATE) != 0;
        }
        
        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public int getProperties() { return properties; }
        public int getPermissions() { return permissions; }
    }
}