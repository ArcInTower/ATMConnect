package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import com.atmconnect.domain.constants.SecurityConstants;
import com.atmconnect.infrastructure.security.SecureMessageProtocol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * ATM BLE Peripheral implementation that acts as a GATT server.
 * 
 * <p>This class implements the ATM side of the BLE architecture where:
 * <ul>
 *   <li>ATM = BLE Peripheral (advertises services, accepts connections)</li>
 *   <li>Mobile = BLE Central (scans for ATMs, initiates connections)</li>
 * </ul>
 * 
 * <p>The peripheral provides GATT services with characteristics for:
 * <ul>
 *   <li>Authentication</li>
 *   <li>Transaction processing</li>
 *   <li>Status monitoring</li>
 *   <li>Certificate distribution</li>
 * </ul>
 * 
 * @see /docs/BLE_ARCHITECTURE.md
 */
@Slf4j
@Service
public class ATMBLEPeripheral {
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final AtomicBoolean isAdvertising = new AtomicBoolean(false);
    private final AtomicBoolean isServerRunning = new AtomicBoolean(false);
    private final AtomicReference<String> atmCode = new AtomicReference<>("001");
    private final AtomicReference<ATMStatus> currentStatus = new AtomicReference<>(ATMStatus.AVAILABLE);
    
    // Connected devices and their characteristics
    private final Map<String, ConnectedDevice> connectedDevices = new ConcurrentHashMap<>();
    private final Map<String, CharacteristicData> characteristicValues = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<BLEEvent> eventQueue = new ConcurrentLinkedQueue<>();
    
    // GATT Service and Characteristics simulation
    private GATTService atmService;
    private GATTCharacteristic authCharacteristic;
    private GATTCharacteristic transactionCharacteristic;
    private GATTCharacteristic statusCharacteristic;
    private GATTCharacteristic certificateCharacteristic;
    
    /**
     * Checks if the GATT server is running.
     * 
     * @return true if server is running
     */
    public boolean isServerRunning() {
        return isServerRunning.get();
    }
    
    /**
     * Checks if BLE advertising is active.
     * 
     * @return true if advertising
     */
    public boolean isAdvertising() {
        return isAdvertising.get();
    }
    
    /**
     * Initializes the ATM BLE Peripheral with GATT server setup.
     * 
     * @param atmCode unique identifier for this ATM
     * @throws BLEException if initialization fails
     */
    public void initialize(String atmCode) throws BLEException {
        log.info("Initializing ATM BLE Peripheral for ATM: {}", atmCode);
        
        try {
            this.atmCode.set(atmCode);
            
            // Initialize GATT Service
            setupGATTService();
            
            // Start background tasks
            startStatusMonitoring();
            startEventProcessing();
            
            this.isServerRunning.set(true);
            log.info("ATM BLE Peripheral initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize ATM BLE Peripheral: {}", e.getMessage(), e);
            throw new BLEException("Peripheral initialization failed", BLEConstants.ERROR_ADVERTISING_FAILED);
        }
    }
    
    /**
     * Starts BLE advertising to make this ATM discoverable by mobile devices.
     * 
     * @throws BLEException if advertising cannot be started
     */
    public void startAdvertising() throws BLEException {
        if (!isServerRunning.get()) {
            throw new BLEException("GATT server not running", BLEConstants.ERROR_ADVERTISING_FAILED);
        }
        
        if (isAdvertising.get()) {
            log.warn("BLE advertising already active");
            return;
        }
        
        try {
            log.info("Starting BLE advertising for ATM: {}", atmCode.get());
            
            // Configure advertising data
            AdvertisingData advertisingData = createAdvertisingData();
            
            // Start advertising with configured parameters
            boolean started = startBLEAdvertising(advertisingData);
            
            if (started) {
                isAdvertising.set(true);
                log.info("BLE advertising started successfully - ATM is now discoverable");
                
                // Schedule advertising refresh
                scheduler.scheduleAtFixedRate(
                    this::refreshAdvertising,
                    BLEConstants.ADVERTISING_INTERVAL_MS,
                    BLEConstants.ADVERTISING_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
                );
            } else {
                throw new BLEException("Failed to start BLE advertising", BLEConstants.ERROR_ADVERTISING_FAILED);
            }
            
        } catch (Exception e) {
            log.error("Error starting BLE advertising: {}", e.getMessage(), e);
            throw new BLEException("Advertising startup failed", BLEConstants.ERROR_ADVERTISING_FAILED);
        }
    }
    
    /**
     * Stops BLE advertising and disconnects all clients.
     */
    public void stopAdvertising() {
        if (!isAdvertising.get()) {
            return;
        }
        
        log.info("Stopping BLE advertising for ATM: {}", atmCode.get());
        
        try {
            // Stop advertising
            stopBLEAdvertising();
            isAdvertising.set(false);
            
            // Disconnect all clients
            disconnectAllClients();
            
            log.info("BLE advertising stopped successfully");
            
        } catch (Exception e) {
            log.error("Error stopping BLE advertising: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Handles incoming connection from a mobile device.
     * 
     * @param deviceId unique identifier of connecting device
     * @param deviceAddress BLE address of connecting device
     * @return connection result
     */
    public ConnectionResult handleConnection(String deviceId, String deviceAddress) {
        log.info("Handling connection request from device: {} ({})", deviceId, deviceAddress);
        
        try {
            // Check connection limits
            if (connectedDevices.size() >= BLEConstants.CONNECTION_TIMEOUT_MS) {
                log.warn("Maximum connections reached, rejecting connection from: {}", deviceId);
                return ConnectionResult.rejected("Maximum connections reached");
            }
            
            // Validate device
            if (!isDeviceAllowed(deviceId, deviceAddress)) {
                log.warn("Device not allowed to connect: {}", deviceId);
                return ConnectionResult.rejected("Device not authorized");
            }
            
            // Create connection
            ConnectedDevice device = new ConnectedDevice(deviceId, deviceAddress, Instant.now());
            connectedDevices.put(deviceId, device);
            
            // Setup connection parameters
            configureConnectionParameters(deviceId);
            
            // Start connection timeout
            scheduleConnectionTimeout(deviceId);
            
            log.info("Connection established with device: {}", deviceId);
            return ConnectionResult.accepted(device);
            
        } catch (Exception e) {
            log.error("Error handling connection from {}: {}", deviceId, e.getMessage(), e);
            return ConnectionResult.rejected("Connection error: " + e.getMessage());
        }
    }
    
    /**
     * Handles disconnection of a mobile device.
     * 
     * @param deviceId identifier of disconnecting device
     */
    public void handleDisconnection(String deviceId) {
        log.info("Handling disconnection for device: {}", deviceId);
        
        ConnectedDevice device = connectedDevices.remove(deviceId);
        if (device != null) {
            // Clean up device-specific data
            cleanupDeviceData(deviceId);
            log.info("Device disconnected: {}", deviceId);
        }
    }
    
    /**
     * Processes a characteristic write request from a mobile device.
     * 
     * @param deviceId requesting device
     * @param characteristicUuid characteristic being written
     * @param data data being written
     * @return write response
     */
    public WriteResponse handleCharacteristicWrite(String deviceId, String characteristicUuid, byte[] data) {
        log.debug("Handling characteristic write from {}: {} ({} bytes)", 
                 deviceId, characteristicUuid, data.length);
        
        try {
            // Validate device connection
            ConnectedDevice device = connectedDevices.get(deviceId);
            if (device == null) {
                return WriteResponse.error("Device not connected", BLEConstants.ERROR_CONNECTION_FAILED);
            }
            
            // Route to appropriate handler based on characteristic
            switch (characteristicUuid) {
                case BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID:
                    return handleAuthenticationWrite(deviceId, data);
                    
                case BLEConstants.TRANSACTION_CHARACTERISTIC_UUID:
                    return handleTransactionWrite(deviceId, data);
                    
                default:
                    log.warn("Write to unsupported characteristic: {}", characteristicUuid);
                    return WriteResponse.error("Characteristic not writable", 
                                             BLEConstants.ERROR_CHARACTERISTIC_NOT_FOUND);
            }
            
        } catch (Exception e) {
            log.error("Error handling characteristic write: {}", e.getMessage(), e);
            return WriteResponse.error("Write processing failed", BLEConstants.ERROR_GATT_SERVICE_NOT_FOUND);
        }
    }
    
    /**
     * Processes a characteristic read request from a mobile device.
     * 
     * @param deviceId requesting device
     * @param characteristicUuid characteristic being read
     * @return read response with data
     */
    public ReadResponse handleCharacteristicRead(String deviceId, String characteristicUuid) {
        log.debug("Handling characteristic read from {}: {}", deviceId, characteristicUuid);
        
        try {
            // Validate device connection
            ConnectedDevice device = connectedDevices.get(deviceId);
            if (device == null) {
                return ReadResponse.error("Device not connected", BLEConstants.ERROR_CONNECTION_FAILED);
            }
            
            // Route to appropriate handler based on characteristic
            switch (characteristicUuid) {
                case BLEConstants.STATUS_CHARACTERISTIC_UUID:
                    return handleStatusRead(deviceId);
                    
                case BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID:
                    return handleCertificateRead(deviceId);
                    
                case BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID:
                    return handleAuthenticationRead(deviceId);
                    
                default:
                    log.warn("Read from unsupported characteristic: {}", characteristicUuid);
                    return ReadResponse.error("Characteristic not readable", 
                                            BLEConstants.ERROR_CHARACTERISTIC_NOT_FOUND);
            }
            
        } catch (Exception e) {
            log.error("Error handling characteristic read: {}", e.getMessage(), e);
            return ReadResponse.error("Read processing failed", BLEConstants.ERROR_GATT_SERVICE_NOT_FOUND);
        }
    }
    
    /**
     * Sends a notification to a connected device.
     * 
     * @param deviceId target device
     * @param characteristicUuid characteristic to notify on
     * @param data notification data
     * @return true if notification sent successfully
     */
    public boolean sendNotification(String deviceId, String characteristicUuid, byte[] data) {
        log.debug("Sending notification to {}: {} ({} bytes)", deviceId, characteristicUuid, data.length);
        
        try {
            ConnectedDevice device = connectedDevices.get(deviceId);
            if (device == null) {
                log.warn("Cannot send notification - device not connected: {}", deviceId);
                return false;
            }
            
            // Check if device has enabled notifications for this characteristic
            if (!device.isNotificationEnabled(characteristicUuid)) {
                log.warn("Notifications not enabled for characteristic: {}", characteristicUuid);
                return false;
            }
            
            // Send notification (simulated)
            boolean sent = sendBLENotification(deviceId, characteristicUuid, data);
            
            if (sent) {
                log.debug("Notification sent successfully to: {}", deviceId);
            } else {
                log.warn("Failed to send notification to: {}", deviceId);
            }
            
            return sent;
            
        } catch (Exception e) {
            log.error("Error sending notification to {}: {}", deviceId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Updates the ATM status and notifies connected devices.
     * 
     * @param newStatus new ATM status
     */
    public void updateATMStatus(ATMStatus newStatus) {
        ATMStatus oldStatus = currentStatus.getAndSet(newStatus);
        
        if (oldStatus != newStatus) {
            log.info("ATM status changed: {} -> {}", oldStatus, newStatus);
            
            // Notify all connected devices
            notifyStatusChange(newStatus);
            
            // Update advertising data if needed
            if (isAdvertising.get()) {
                updateAdvertisingData();
            }
        }
    }
    
    /**
     * Shuts down the BLE peripheral and cleans up resources.
     */
    public void shutdown() {
        log.info("Shutting down ATM BLE Peripheral");
        
        try {
            // Stop advertising
            stopAdvertising();
            
            // Disconnect all clients
            disconnectAllClients();
            
            // Stop background tasks
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            isServerRunning.set(false);
            log.info("ATM BLE Peripheral shutdown complete");
            
        } catch (Exception e) {
            log.error("Error during BLE peripheral shutdown: {}", e.getMessage(), e);
        }
    }
    
    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================
    
    private void setupGATTService() {
        log.debug("Setting up GATT service and characteristics");
        
        // Create ATM service
        atmService = new GATTService(BLEConstants.ATM_SERVICE_UUID, true);
        
        // Create characteristics with proper properties and permissions
        authCharacteristic = new GATTCharacteristic(
            BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID,
            BLEConstants.PROPERTY_READ | BLEConstants.PROPERTY_WRITE | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_READ_ENCRYPTED_MITM | BLEConstants.PERMISSION_WRITE_ENCRYPTED_MITM
        );
        
        transactionCharacteristic = new GATTCharacteristic(
            BLEConstants.TRANSACTION_CHARACTERISTIC_UUID,
            BLEConstants.PROPERTY_WRITE | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_WRITE_ENCRYPTED_MITM
        );
        
        statusCharacteristic = new GATTCharacteristic(
            BLEConstants.STATUS_CHARACTERISTIC_UUID,
            BLEConstants.PROPERTY_READ | BLEConstants.PROPERTY_NOTIFY,
            BLEConstants.PERMISSION_READ
        );
        
        certificateCharacteristic = new GATTCharacteristic(
            BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID,
            BLEConstants.PROPERTY_READ,
            BLEConstants.PERMISSION_READ
        );
        
        // Add characteristics to service
        atmService.addCharacteristic(authCharacteristic);
        atmService.addCharacteristic(transactionCharacteristic);
        atmService.addCharacteristic(statusCharacteristic);
        atmService.addCharacteristic(certificateCharacteristic);
        
        log.debug("GATT service setup complete");
    }
    
    private AdvertisingData createAdvertisingData() {
        String localName = BLEConstants.generateATMLocalName(atmCode.get());
        
        return AdvertisingData.builder()
            .localName(localName)
            .serviceUuid(BLEConstants.ATM_SERVICE_UUID)
            .txPowerLevel(BLEConstants.TX_POWER_LEVEL_DBM)
            .manufacturerData(createManufacturerData())
            .connectable(true)
            .build();
    }
    
    private byte[] createManufacturerData() {
        // Format: [Company ID (2 bytes)] [ATM Type (1 byte)] [Capabilities (1 byte)] [Status (1 byte)] [Cash Level (1 byte)]
        byte[] data = new byte[6];
        
        // Company ID (little endian)
        data[0] = (byte) (BLEConstants.COMPANY_ID & 0xFF);
        data[1] = (byte) ((BLEConstants.COMPANY_ID >> 8) & 0xFF);
        
        // ATM Type
        data[2] = BLEConstants.ATM_TYPE_STANDARD;
        
        // Capabilities
        data[3] = BLEConstants.CAPABILITY_ALL;
        
        // Status
        data[4] = mapStatusToByte(currentStatus.get());
        
        // Cash Level (simulated)
        data[5] = BLEConstants.CASH_LEVEL_HIGH;
        
        return data;
    }
    
    private byte mapStatusToByte(ATMStatus status) {
        switch (status) {
            case AVAILABLE: return BLEConstants.STATUS_AVAILABLE;
            case BUSY: return BLEConstants.STATUS_BUSY;
            case OUT_OF_SERVICE: return BLEConstants.STATUS_OUT_OF_SERVICE;
            case MAINTENANCE: return BLEConstants.STATUS_MAINTENANCE;
            default: return BLEConstants.STATUS_OUT_OF_SERVICE;
        }
    }
    
    private void startStatusMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Monitor ATM health and update status if needed
                monitorATMHealth();
            } catch (Exception e) {
                log.error("Error in status monitoring: {}", e.getMessage(), e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    private void startEventProcessing() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                processPendingEvents();
            } catch (Exception e) {
                log.error("Error processing BLE events: {}", e.getMessage(), e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    private void monitorATMHealth() {
        // Simulate ATM health monitoring
        // In real implementation, this would check hardware status
        log.trace("Monitoring ATM health - Status: {}", currentStatus.get());
    }
    
    private void processPendingEvents() {
        BLEEvent event;
        while ((event = eventQueue.poll()) != null) {
            handleBLEEvent(event);
        }
    }
    
    private void handleBLEEvent(BLEEvent event) {
        log.debug("Processing BLE event: {}", event.getType());
        // Process different types of BLE events
    }
    
    // Simulated BLE operations (would use actual BLE APIs in real implementation)
    private boolean startBLEAdvertising(AdvertisingData data) {
        log.debug("Starting BLE advertising with data: {}", data.getLocalName());
        return true; // Simulate successful advertising start
    }
    
    private void stopBLEAdvertising() {
        log.debug("Stopping BLE advertising");
    }
    
    private boolean sendBLENotification(String deviceId, String characteristicUuid, byte[] data) {
        log.trace("Sending BLE notification: {} bytes to {}", data.length, deviceId);
        return true; // Simulate successful notification
    }
    
    private void refreshAdvertising() {
        if (isAdvertising.get()) {
            log.trace("Refreshing BLE advertising data");
            // Update advertising data periodically
        }
    }
    
    private void updateAdvertisingData() {
        if (isAdvertising.get()) {
            log.debug("Updating advertising data due to status change");
            // Update manufacturer data with new status
        }
    }
    
    private boolean isDeviceAllowed(String deviceId, String deviceAddress) {
        // Implement device authorization logic
        // For now, allow all devices
        return true;
    }
    
    private void configureConnectionParameters(String deviceId) {
        log.debug("Configuring connection parameters for device: {}", deviceId);
        // Set connection interval, latency, timeout
    }
    
    private void scheduleConnectionTimeout(String deviceId) {
        scheduler.schedule(() -> {
            ConnectedDevice device = connectedDevices.get(deviceId);
            if (device != null && device.isIdle()) {
                log.info("Disconnecting idle device: {}", deviceId);
                handleDisconnection(deviceId);
            }
        }, BLEConstants.IDLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }
    
    private void disconnectAllClients() {
        log.info("Disconnecting all connected devices");
        List<String> deviceIds = new ArrayList<>(connectedDevices.keySet());
        deviceIds.forEach(this::handleDisconnection);
    }
    
    private void cleanupDeviceData(String deviceId) {
        // Remove any device-specific cached data
        characteristicValues.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(deviceId + ":"));
    }
    
    private void notifyStatusChange(ATMStatus newStatus) {
        byte[] statusData = new byte[]{mapStatusToByte(newStatus)};
        
        connectedDevices.keySet().forEach(deviceId -> {
            sendNotification(deviceId, BLEConstants.STATUS_CHARACTERISTIC_UUID, statusData);
        });
    }
    
    // Characteristic handlers
    private WriteResponse handleAuthenticationWrite(String deviceId, byte[] data) {
        log.debug("Processing authentication write from device: {}", deviceId);
        
        try {
            // Parse and validate authentication data
            SecureMessageProtocol message = SecureMessageProtocol.fromBytes(data);
            
            if (message.isExpired()) {
                return WriteResponse.error("Authentication message expired", 
                                         BLEConstants.ERROR_AUTHENTICATION_FAILED);
            }
            
            // Process authentication
            boolean authenticated = processAuthentication(deviceId, message);
            
            if (authenticated) {
                // Send success notification
                byte[] response = createAuthenticationResponse(true);
                sendNotification(deviceId, BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, response);
                return WriteResponse.success();
            } else {
                return WriteResponse.error("Authentication failed", 
                                         BLEConstants.ERROR_AUTHENTICATION_FAILED);
            }
            
        } catch (Exception e) {
            log.error("Authentication processing error: {}", e.getMessage(), e);
            return WriteResponse.error("Authentication processing failed", 
                                     BLEConstants.ERROR_AUTHENTICATION_FAILED);
        }
    }
    
    private WriteResponse handleTransactionWrite(String deviceId, byte[] data) {
        log.debug("Processing transaction write from device: {}", deviceId);
        
        try {
            // Parse transaction request
            SecureMessageProtocol message = SecureMessageProtocol.fromBytes(data);
            
            if (message.isExpired()) {
                return WriteResponse.error("Transaction message expired", 
                                         BLEConstants.ERROR_TRANSACTION_TIMEOUT);
            }
            
            // Process transaction
            TransactionResult result = processTransaction(deviceId, message);
            
            // Send result notification
            byte[] response = createTransactionResponse(result);
            sendNotification(deviceId, BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, response);
            
            return WriteResponse.success();
            
        } catch (Exception e) {
            log.error("Transaction processing error: {}", e.getMessage(), e);
            return WriteResponse.error("Transaction processing failed", 
                                     BLEConstants.ERROR_TRANSACTION_TIMEOUT);
        }
    }
    
    private ReadResponse handleStatusRead(String deviceId) {
        log.debug("Processing status read from device: {}", deviceId);
        
        try {
            byte[] statusData = createStatusData();
            return ReadResponse.success(statusData);
            
        } catch (Exception e) {
            log.error("Status read error: {}", e.getMessage(), e);
            return ReadResponse.error("Status read failed", BLEConstants.ERROR_GATT_SERVICE_NOT_FOUND);
        }
    }
    
    private ReadResponse handleCertificateRead(String deviceId) {
        log.debug("Processing certificate read from device: {}", deviceId);
        
        try {
            byte[] certificateData = getATMCertificate();
            return ReadResponse.success(certificateData);
            
        } catch (Exception e) {
            log.error("Certificate read error: {}", e.getMessage(), e);
            return ReadResponse.error("Certificate read failed", 
                                    BLEConstants.ERROR_CERTIFICATE_INVALID);
        }
    }
    
    private ReadResponse handleAuthenticationRead(String deviceId) {
        log.debug("Processing authentication read from device: {}", deviceId);
        
        try {
            // Return authentication challenge or status
            byte[] authData = createAuthenticationChallenge(deviceId);
            return ReadResponse.success(authData);
            
        } catch (Exception e) {
            log.error("Authentication read error: {}", e.getMessage(), e);
            return ReadResponse.error("Authentication read failed", 
                                    BLEConstants.ERROR_AUTHENTICATION_FAILED);
        }
    }
    
    private boolean processAuthentication(String deviceId, SecureMessageProtocol message) {
        // Implement authentication logic
        log.debug("Processing authentication for device: {}", deviceId);
        return true; // Simulate successful authentication
    }
    
    private TransactionResult processTransaction(String deviceId, SecureMessageProtocol message) {
        // Implement transaction processing logic
        log.debug("Processing transaction for device: {}", deviceId);
        return new TransactionResult(true, "Transaction completed successfully");
    }
    
    private byte[] createAuthenticationResponse(boolean success) {
        // Create authentication response message
        return new byte[]{(byte) (success ? 1 : 0)};
    }
    
    private byte[] createTransactionResponse(TransactionResult result) {
        // Create transaction response message
        return result.isSuccess() ? new byte[]{1} : new byte[]{0};
    }
    
    private byte[] createStatusData() {
        // Create status information
        return new byte[]{mapStatusToByte(currentStatus.get())};
    }
    
    private byte[] getATMCertificate() {
        // Return ATM X.509 certificate
        return "MOCK_CERTIFICATE_DATA".getBytes();
    }
    
    private byte[] createAuthenticationChallenge(String deviceId) {
        // Create authentication challenge for device
        return ("CHALLENGE_" + deviceId).getBytes();
    }
    
    // ============================================================================
    // INNER CLASSES AND ENUMS
    // ============================================================================
    
    public enum ATMStatus {
        AVAILABLE, BUSY, OUT_OF_SERVICE, MAINTENANCE
    }
    
    private static class ConnectedDevice {
        private final String deviceId;
        private final String deviceAddress;
        private final Instant connectionTime;
        private final Map<String, Boolean> notificationStates = new ConcurrentHashMap<>();
        private volatile Instant lastActivity;
        
        public ConnectedDevice(String deviceId, String deviceAddress, Instant connectionTime) {
            this.deviceId = deviceId;
            this.deviceAddress = deviceAddress;
            this.connectionTime = connectionTime;
            this.lastActivity = connectionTime;
        }
        
        public boolean isNotificationEnabled(String characteristicUuid) {
            return notificationStates.getOrDefault(characteristicUuid, false);
        }
        
        public void setNotificationEnabled(String characteristicUuid, boolean enabled) {
            notificationStates.put(characteristicUuid, enabled);
            updateActivity();
        }
        
        public boolean isIdle() {
            return Instant.now().minusMillis(BLEConstants.IDLE_TIMEOUT_MS).isAfter(lastActivity);
        }
        
        public void updateActivity() {
            this.lastActivity = Instant.now();
        }
    }
    
    private static class CharacteristicData {
        private final byte[] value;
        private final Instant timestamp;
        
        public CharacteristicData(byte[] value) {
            this.value = value.clone();
            this.timestamp = Instant.now();
        }
        
        public byte[] getValue() {
            return value.clone();
        }
        
        public boolean isExpired(long maxAgeMs) {
            return Instant.now().minusMillis(maxAgeMs).isAfter(timestamp);
        }
    }
    
    // Response classes
    public static class ConnectionResult {
        private final boolean accepted;
        private final String message;
        private final ConnectedDevice device;
        
        private ConnectionResult(boolean accepted, String message, ConnectedDevice device) {
            this.accepted = accepted;
            this.message = message;
            this.device = device;
        }
        
        public static ConnectionResult accepted(ConnectedDevice device) {
            return new ConnectionResult(true, "Connection accepted", device);
        }
        
        public static ConnectionResult rejected(String reason) {
            return new ConnectionResult(false, reason, null);
        }
        
        public boolean isAccepted() { return accepted; }
        public String getMessage() { return message; }
        public ConnectedDevice getDevice() { return device; }
    }
    
    public static class WriteResponse {
        private final boolean success;
        private final String message;
        private final int errorCode;
        
        private WriteResponse(boolean success, String message, int errorCode) {
            this.success = success;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        public static WriteResponse success() {
            return new WriteResponse(true, "Write successful", 0);
        }
        
        public static WriteResponse error(String message, int errorCode) {
            return new WriteResponse(false, message, errorCode);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getErrorCode() { return errorCode; }
    }
    
    public static class ReadResponse {
        private final boolean success;
        private final byte[] data;
        private final String message;
        private final int errorCode;
        
        private ReadResponse(boolean success, byte[] data, String message, int errorCode) {
            this.success = success;
            this.data = data != null ? data.clone() : null;
            this.message = message;
            this.errorCode = errorCode;
        }
        
        public static ReadResponse success(byte[] data) {
            return new ReadResponse(true, data, "Read successful", 0);
        }
        
        public static ReadResponse error(String message, int errorCode) {
            return new ReadResponse(false, null, message, errorCode);
        }
        
        public boolean isSuccess() { return success; }
        public byte[] getData() { return data != null ? data.clone() : null; }
        public String getMessage() { return message; }
        public int getErrorCode() { return errorCode; }
    }
    
    private static class TransactionResult {
        private final boolean success;
        private final String message;
        
        public TransactionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    private static class BLEEvent {
        private final String type;
        private final String deviceId;
        private final Object data;
        private final Instant timestamp;
        
        public BLEEvent(String type, String deviceId, Object data) {
            this.type = type;
            this.deviceId = deviceId;
            this.data = data;
            this.timestamp = Instant.now();
        }
        
        public String getType() { return type; }
        public String getDeviceId() { return deviceId; }
        public Object getData() { return data; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    // GATT simulation classes
    private static class GATTService {
        private final String uuid;
        private final boolean primary;
        private final List<GATTCharacteristic> characteristics = new ArrayList<>();
        
        public GATTService(String uuid, boolean primary) {
            this.uuid = uuid;
            this.primary = primary;
        }
        
        public void addCharacteristic(GATTCharacteristic characteristic) {
            characteristics.add(characteristic);
        }
        
        public String getUuid() { return uuid; }
        public boolean isPrimary() { return primary; }
        public List<GATTCharacteristic> getCharacteristics() { return characteristics; }
    }
    
    private static class GATTCharacteristic {
        private final String uuid;
        private final int properties;
        private final int permissions;
        
        public GATTCharacteristic(String uuid, int properties, int permissions) {
            this.uuid = uuid;
            this.properties = properties;
            this.permissions = permissions;
        }
        
        public String getUuid() { return uuid; }
        public int getProperties() { return properties; }
        public int getPermissions() { return permissions; }
    }
    
    private static class AdvertisingData {
        private final String localName;
        private final String serviceUuid;
        private final int txPowerLevel;
        private final byte[] manufacturerData;
        private final boolean connectable;
        
        private AdvertisingData(Builder builder) {
            this.localName = builder.localName;
            this.serviceUuid = builder.serviceUuid;
            this.txPowerLevel = builder.txPowerLevel;
            this.manufacturerData = builder.manufacturerData;
            this.connectable = builder.connectable;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public String getLocalName() { return localName; }
        public String getServiceUuid() { return serviceUuid; }
        public int getTxPowerLevel() { return txPowerLevel; }
        public byte[] getManufacturerData() { return manufacturerData != null ? manufacturerData.clone() : null; }
        public boolean isConnectable() { return connectable; }
        
        public static class Builder {
            private String localName;
            private String serviceUuid;
            private int txPowerLevel;
            private byte[] manufacturerData;
            private boolean connectable;
            
            public Builder localName(String localName) {
                this.localName = localName;
                return this;
            }
            
            public Builder serviceUuid(String serviceUuid) {
                this.serviceUuid = serviceUuid;
                return this;
            }
            
            public Builder txPowerLevel(int txPowerLevel) {
                this.txPowerLevel = txPowerLevel;
                return this;
            }
            
            public Builder manufacturerData(byte[] manufacturerData) {
                this.manufacturerData = manufacturerData != null ? manufacturerData.clone() : null;
                return this;
            }
            
            public Builder connectable(boolean connectable) {
                this.connectable = connectable;
                return this;
            }
            
            public AdvertisingData build() {
                return new AdvertisingData(this);
            }
        }
    }
    
    /**
     * Custom exception for BLE-related errors.
     */
    public static class BLEException extends Exception {
        private final int errorCode;
        
        public BLEException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public BLEException(String message, int errorCode, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public int getErrorCode() {
            return errorCode;
        }
    }
}