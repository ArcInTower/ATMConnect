package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import com.atmconnect.domain.ports.outbound.BluetoothService;
import com.atmconnect.infrastructure.security.CryptoService;
import com.atmconnect.infrastructure.security.SecureMessageProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced Bluetooth service implementation that supports both Central and Peripheral modes.
 * 
 * <p>This service acts as:
 * <ul>
 *   <li>BLE Central - for mobile applications scanning and connecting to ATMs</li>
 *   <li>BLE Peripheral - for ATM devices advertising services and accepting connections</li>
 * </ul>
 * 
 * <p>The implementation follows the documented BLE architecture where ATMs are peripherals
 * and mobile devices are centrals, providing proper GATT services and characteristics
 * with appropriate security measures.
 */
@Service
@Slf4j
public class BluetoothServiceImpl implements BluetoothService {
    
    private final Map<String, BluetoothConnectionImpl> activeConnections = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    private CryptoService cryptoService;
    
    @Autowired
    private ATMBLEPeripheral atmPeripheral;
    
    @Value("${atmconnect.bluetooth.mode:central}")
    private String operationMode; // "central", "peripheral", or "both"
    
    @Value("${atmconnect.atm.code:001}")
    private String atmCode;
    
    private volatile boolean isInitialized = false;
    private volatile boolean isScanning = false;
    private volatile boolean isPeripheralActive = false;
    
    public BluetoothServiceImpl() {
        // Initialization moved to @PostConstruct to ensure proper dependency injection
    }
    
    /**
     * Initializes the Bluetooth service based on the configured operation mode.
     * 
     * @throws RuntimeException if initialization fails
     */
    @javax.annotation.PostConstruct
    public void initialize() {
        if (isInitialized) {
            return;
        }
        
        log.info("Initializing Bluetooth service in {} mode", operationMode);
        
        try {
            // Initialize based on operation mode
            switch (operationMode.toLowerCase()) {
                case "central":
                    initializeCentralMode();
                    break;
                case "peripheral":
                    initializePeripheralMode();
                    break;
                case "both":
                    initializeCentralMode();
                    initializePeripheralMode();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operation mode: " + operationMode);
            }
            
            startConnectionMonitor();
            isInitialized = true;
            
            log.info("Bluetooth service initialized successfully in {} mode", operationMode);
            
        } catch (Exception e) {
            log.error("Failed to initialize Bluetooth service: {}", e.getMessage(), e);
            throw new RuntimeException("Bluetooth service initialization failed", e);
        }
    }
    
    /**
     * Initializes BLE Central mode for scanning and connecting to ATM peripherals.
     */
    private void initializeCentralMode() {
        log.info("Initializing BLE Central mode");
        // Central mode initialization - scanning capabilities
        // In real implementation, this would initialize BLE adapter for scanning
    }
    
    /**
     * Initializes BLE Peripheral mode for ATM advertising and GATT server.
     */
    private void initializePeripheralMode() {
        log.info("Initializing BLE Peripheral mode for ATM: {}", atmCode);
        
        try {
            // Initialize ATM peripheral
            atmPeripheral.initialize(atmCode);
            
            // Start advertising
            atmPeripheral.startAdvertising();
            isPeripheralActive = true;
            
            log.info("ATM BLE Peripheral initialized and advertising");
            
        } catch (Exception e) {
            log.error("Failed to initialize BLE Peripheral mode: {}", e.getMessage(), e);
            throw new RuntimeException("Peripheral mode initialization failed", e);
        }
    }
    
    /**
     * Starts connection monitoring and cleanup tasks.
     */
    private void startConnectionMonitor() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                // Clean up inactive connections
                activeConnections.values().stream()
                    .filter(conn -> !conn.isConnected())
                    .forEach(conn -> {
                        activeConnections.remove(conn.getDeviceAddress());
                        log.debug("Removed inactive connection: {}", conn.getDeviceAddress());
                    });
                
                // Monitor peripheral health if active
                if (isPeripheralActive) {
                    monitorPeripheralHealth();
                }
                
            } catch (Exception e) {
                log.error("Error in connection monitoring: {}", e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Monitors the health of the ATM peripheral and restarts if necessary.
     */
    private void monitorPeripheralHealth() {
        // Check if peripheral is still advertising and healthy
        // Restart if needed
        log.trace("Monitoring ATM peripheral health");
    }
    
    @Override
    public CompletableFuture<List<BluetoothDevice>> scanForDevices(int timeoutSeconds) {
        if (!isInitialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Bluetooth service not initialized"));
        }
        
        if (!operationMode.equals("central") && !operationMode.equals("both")) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Central mode not enabled"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting BLE scan for ATM devices - timeout: {}s", timeoutSeconds);
            
            if (isScanning) {
                throw new IllegalStateException("Scan already in progress");
            }
            
            try {
                isScanning = true;
                List<BluetoothDevice> discoveredDevices = performBLEScan(timeoutSeconds);
                
                log.info("BLE scan completed - found {} ATM devices", discoveredDevices.size());
                return discoveredDevices;
                
            } catch (Exception e) {
                log.error("BLE scan failed: {}", e.getMessage(), e);
                throw new RuntimeException("Device scan failed", e);
            } finally {
                isScanning = false;
            }
        }, executorService);
    }
    
    /**
     * Performs the actual BLE scan for ATM devices.
     * 
     * @param timeoutSeconds scan timeout
     * @return list of discovered ATM devices
     */
    private List<BluetoothDevice> performBLEScan(int timeoutSeconds) {
        List<BluetoothDevice> devices = new ArrayList<>();
        
        try {
            log.debug("Starting BLE scan with filter for ATM service UUID: {}", 
                     BLEConstants.ATM_SERVICE_UUID);
            
            // Configure scan parameters
            BLEScanParameters scanParams = new BLEScanParameters(
                BLEConstants.SCAN_WINDOW_MS,
                BLEConstants.SCAN_INTERVAL_MS,
                BLEConstants.SCAN_MODE_BALANCED,
                Collections.singletonList(BLEConstants.ATM_SERVICE_UUID)
            );
            
            // Perform scan (simulated for now)
            devices.addAll(simulateATMDiscovery());
            
            // Filter devices by RSSI and range
            devices = devices.stream()
                .filter(device -> BLEConstants.isRssiAcceptable(device.getRssi()))
                .filter(device -> isATMDevice(device))
                .collect(Collectors.toList());
            
            log.debug("Filtered {} ATM devices by RSSI and service criteria", devices.size());
            
        } catch (Exception e) {
            log.error("Error during BLE scan: {}", e.getMessage(), e);
            throw e;
        }
        
        return devices;
    }
    
    /**
     * Simulates ATM device discovery (for development/testing).
     * In production, this would be replaced with actual BLE scanning.
     */
    private List<BluetoothDevice> simulateATMDiscovery() {
        List<BluetoothDevice> devices = new ArrayList<>();
        
        // Simulate discovering ATM devices
        devices.add(new BluetoothDeviceImpl(
            "00:11:22:33:44:55",
            BLEConstants.generateATMLocalName("001"),
            -65,
            true,
            createATMManufacturerData("001")
        ));
        
        devices.add(new BluetoothDeviceImpl(
            "00:11:22:33:44:66",
            BLEConstants.generateATMLocalName("002"),
            -72,
            true,
            createATMManufacturerData("002")
        ));
        
        // Add a device that's too far (weak signal)
        devices.add(new BluetoothDeviceImpl(
            "00:11:22:33:44:77",
            BLEConstants.generateATMLocalName("003"),
            -85, // Below minimum RSSI threshold
            true,
            createATMManufacturerData("003")
        ));
        
        return devices;
    }
    
    /**
     * Creates manufacturer data for simulated ATM devices.
     */
    private byte[] createATMManufacturerData(String atmCode) {
        byte[] data = new byte[6];
        
        // Company ID (little endian)
        data[0] = (byte) (BLEConstants.COMPANY_ID & 0xFF);
        data[1] = (byte) ((BLEConstants.COMPANY_ID >> 8) & 0xFF);
        
        // ATM Type
        data[2] = BLEConstants.ATM_TYPE_STANDARD;
        
        // Capabilities
        data[3] = BLEConstants.CAPABILITY_ALL;
        
        // Status
        data[4] = BLEConstants.STATUS_AVAILABLE;
        
        // Cash Level
        data[5] = BLEConstants.CASH_LEVEL_HIGH;
        
        return data;
    }
    
    /**
     * Checks if a discovered device is a valid ATM device.
     */
    private boolean isATMDevice(BluetoothDevice device) {
        // Check if device name starts with ATM prefix
        String name = device.getName();
        if (name == null || !name.startsWith(BLEConstants.ATM_LOCAL_NAME_PREFIX)) {
            return false;
        }
        
        // Check manufacturer data if available
        byte[] manufacturerData = device.getManufacturerData();
        if (manufacturerData != null && manufacturerData.length >= 2) {
            // Check company ID
            int companyId = (manufacturerData[1] << 8) | (manufacturerData[0] & 0xFF);
            return companyId == BLEConstants.COMPANY_ID;
        }
        
        return true; // Allow devices without manufacturer data for now
    }
    
    @Override
    public CompletableFuture<BluetoothConnection> connect(String deviceAddress) {
        if (!isInitialized) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Bluetooth service not initialized"));
        }
        
        if (!operationMode.equals("central") && !operationMode.equals("both")) {
            return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Central mode not enabled"));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            // Check for existing connection
            if (activeConnections.containsKey(deviceAddress)) {
                BluetoothConnectionImpl existing = activeConnections.get(deviceAddress);
                if (existing.isConnected()) {
                    log.info("Reusing existing connection to ATM: {}", deviceAddress);
                    return existing;
                } else {
                    // Remove stale connection
                    activeConnections.remove(deviceAddress);
                }
            }
            
            log.info("Establishing new BLE connection to ATM: {}", deviceAddress);
            
            try {
                // Validate device address
                if (!isValidBLEAddress(deviceAddress)) {
                    throw new IllegalArgumentException("Invalid BLE device address: " + deviceAddress);
                }
                
                // Check connection limits
                if (activeConnections.size() >= BLEConstants.CONNECTION_TIMEOUT_MS) {
                    throw new IllegalStateException("Maximum concurrent connections exceeded");
                }
                
                // Establish GATT connection
                BluetoothConnectionImpl connection = establishGATTConnection(deviceAddress);
                
                // Store connection
                activeConnections.put(deviceAddress, connection);
                
                log.info("Successfully connected to ATM: {} (GATT services discovered)", deviceAddress);
                return connection;
                
            } catch (Exception e) {
                log.error("Failed to connect to ATM {}: {}", deviceAddress, e.getMessage(), e);
                throw new RuntimeException("BLE connection failed: " + e.getMessage(), e);
            }
        }, executorService);
    }
    
    /**
     * Establishes a GATT connection to an ATM device.
     */
    private BluetoothConnectionImpl establishGATTConnection(String deviceAddress) 
            throws InterruptedException {
        
        log.debug("Establishing GATT connection to: {}", deviceAddress);
        
        // Simulate connection establishment with proper timing
        Thread.sleep(BLEConstants.CONNECTION_TIMEOUT_MS / 10); // Realistic connection time
        
        // Create connection with GATT client capabilities
        BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
            deviceAddress,
            true,
            cryptoService
        );
        
        // Discover ATM GATT services
        boolean servicesDiscovered = discoverATMServices(connection);
        if (!servicesDiscovered) {
            connection.close();
            throw new RuntimeException("Failed to discover ATM GATT services");
        }
        
        // Configure connection parameters
        configureGATTConnectionParameters(connection);
        
        return connection;
    }
    
    /**
     * Discovers and validates ATM GATT services.
     */
    private boolean discoverATMServices(BluetoothConnectionImpl connection) {
        log.debug("Discovering GATT services for ATM: {}", connection.getDeviceAddress());
        
        try {
            // Simulate service discovery
            Thread.sleep(500); // Service discovery time
            
            // Check for required ATM service
            boolean hasATMService = connection.hasService(BLEConstants.ATM_SERVICE_UUID);
            if (!hasATMService) {
                log.warn("ATM service not found on device: {}", connection.getDeviceAddress());
                return false;
            }
            
            // Verify required characteristics
            String[] requiredCharacteristics = {
                BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID,
                BLEConstants.TRANSACTION_CHARACTERISTIC_UUID,
                BLEConstants.STATUS_CHARACTERISTIC_UUID,
                BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID
            };
            
            for (String characteristicUuid : requiredCharacteristics) {
                if (!connection.hasCharacteristic(characteristicUuid)) {
                    log.warn("Required characteristic {} not found on ATM: {}", 
                           characteristicUuid, connection.getDeviceAddress());
                    return false;
                }
            }
            
            log.debug("All required ATM services and characteristics discovered");
            return true;
            
        } catch (Exception e) {
            log.error("Error during service discovery: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Configures optimal GATT connection parameters.
     */
    private void configureGATTConnectionParameters(BluetoothConnectionImpl connection) {
        log.debug("Configuring GATT connection parameters");
        
        // Set connection parameters for banking operations
        connection.setConnectionInterval(BLEConstants.CONNECTION_INTERVAL_UNITS);
        connection.setSlaveLatency(BLEConstants.SLAVE_LATENCY);
        connection.setSupervisionTimeout(BLEConstants.SUPERVISION_TIMEOUT_UNITS);
        
        // Enable encryption if not already enabled
        connection.enableEncryption();
    }
    
    /**
     * Validates a BLE device address format.
     */
    private boolean isValidBLEAddress(String address) {
        if (address == null || address.length() != 17) {
            return false;
        }
        
        // Check MAC address format: XX:XX:XX:XX:XX:XX
        return address.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }
    
    @Override
    public void disconnect(String deviceAddress) {
        BluetoothConnectionImpl connection = activeConnections.remove(deviceAddress);
        if (connection != null) {
            connection.close();
            log.info("Disconnected from {}", deviceAddress);
        }
    }
    
    @Override
    public boolean isConnected(String deviceAddress) {
        BluetoothConnectionImpl connection = activeConnections.get(deviceAddress);
        return connection != null && connection.isConnected();
    }
    
    @Override
    public CompletableFuture<byte[]> sendSecureMessage(String deviceAddress, byte[] message) {
        return CompletableFuture.supplyAsync(() -> {
            BluetoothConnectionImpl connection = activeConnections.get(deviceAddress);
            if (connection == null || !connection.isConnected()) {
                throw new IllegalStateException("Not connected to device: " + deviceAddress);
            }
            
            try {
                // Build secure message protocol
                SecureMessageProtocol protocol = SecureMessageProtocol.builder()
                    .messageId(UUID.randomUUID().toString())
                    .senderId("APP")
                    .recipientId(deviceAddress)
                    .timestamp(Instant.now().getEpochSecond())
                    .nonce(Base64.getEncoder().encodeToString(
                        cryptoService.generateSecureRandom(16)))
                    .encryptedPayload(Base64.getEncoder().encodeToString(message))
                    .version(SecureMessageProtocol.CURRENT_VERSION)
                    .build();
                
                // Sign the message
                String signatureData = protocol.computeSignatureData();
                byte[] signature = cryptoService.sign(signatureData.getBytes());
                protocol.setSignature(Base64.getEncoder().encodeToString(signature));
                
                // Send and receive response
                byte[] response = connection.sendMessage(protocol.toBytes());
                
                log.debug("Secure message sent to {} and response received", deviceAddress);
                return response;
            } catch (Exception e) {
                log.error("Failed to send secure message to {}", deviceAddress, e);
                throw new RuntimeException("Message sending failed", e);
            }
        }, executorService);
    }
    
    /**
     * Gracefully shuts down the Bluetooth service and all connections.
     */
    @javax.annotation.PreDestroy
    public void shutdown() {
        log.info("Shutting down Bluetooth service");
        
        try {
            // Stop peripheral if active
            if (isPeripheralActive && atmPeripheral != null) {
                atmPeripheral.shutdown();
                isPeripheralActive = false;
            }
            
            // Disconnect all active connections
            List<String> deviceAddresses = new ArrayList<>(activeConnections.keySet());
            for (String deviceAddress : deviceAddresses) {
                disconnect(deviceAddress);
            }
            
            // Shutdown executors
            shutdownExecutors();
            
            isInitialized = false;
            log.info("Bluetooth service shutdown complete");
            
        } catch (Exception e) {
            log.error("Error during Bluetooth service shutdown: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Shuts down executor services gracefully.
     */
    private void shutdownExecutors() {
        try {
            scheduledExecutor.shutdown();
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Updates the ATM status (for peripheral mode).
     */
    public void updateATMStatus(ATMBLEPeripheral.ATMStatus status) {
        if (isPeripheralActive && atmPeripheral != null) {
            atmPeripheral.updateATMStatus(status);
        }
    }
    
    /**
     * Gets the current operation mode.
     */
    public String getOperationMode() {
        return operationMode;
    }
    
    /**
     * Checks if the service is running in peripheral mode.
     */
    public boolean isPeripheralMode() {
        return isPeripheralActive;
    }
    
    /**
     * Checks if the service is currently scanning.
     */
    public boolean isScanning() {
        return isScanning;
    }
    
    /**
     * Gets the number of active connections.
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * Gets a copy of active connection addresses.
     */
    public Set<String> getActiveConnectionAddresses() {
        return new HashSet<>(activeConnections.keySet());
    }
    
    // ============================================================================
    // INNER CLASSES
    // ============================================================================
    
    /**
     * BLE scan parameters configuration.
     */
    private static class BLEScanParameters {
        private final int scanWindow;
        private final int scanInterval;
        private final int scanMode;
        private final List<String> serviceUuids;
        
        public BLEScanParameters(int scanWindow, int scanInterval, int scanMode, 
                               List<String> serviceUuids) {
            this.scanWindow = scanWindow;
            this.scanInterval = scanInterval;
            this.scanMode = scanMode;
            this.serviceUuids = serviceUuids != null ? 
                new ArrayList<>(serviceUuids) : new ArrayList<>();
        }
        
        public int getScanWindow() { return scanWindow; }
        public int getScanInterval() { return scanInterval; }
        public int getScanMode() { return scanMode; }
        public List<String> getServiceUuids() { return new ArrayList<>(serviceUuids); }
    }
}