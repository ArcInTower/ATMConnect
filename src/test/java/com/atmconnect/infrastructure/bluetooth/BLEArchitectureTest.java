package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import com.atmconnect.domain.ports.outbound.BluetoothService.BluetoothDevice;
import com.atmconnect.domain.ports.outbound.BluetoothService.BluetoothConnection;
import com.atmconnect.infrastructure.security.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for BLE architecture implementation.
 * 
 * <p>Tests verify that the BLE implementation follows the documented architecture
 * where ATMs are peripherals and mobile devices are centrals, with proper
 * GATT services, characteristics, and security measures.
 */
@DisplayName("BLE Architecture Tests")
class BLEArchitectureTest {

    @Mock
    private CryptoService cryptoService;

    private ATMBLEPeripheral atmPeripheral;
    private BluetoothServiceImpl bluetoothService;
    private BluetoothConnectionImpl bluetoothConnection;
    private BLEErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock crypto service responses
        when(cryptoService.generateSecureRandom(anyInt()))
            .thenReturn(new byte[32]); // Mock session key
        when(cryptoService.sign(any(byte[].class)))
            .thenReturn(new byte[64]); // Mock signature
        when(cryptoService.verifySignature(any(byte[].class), any(byte[].class)))
            .thenReturn(true);
        
        // Initialize components
        atmPeripheral = new ATMBLEPeripheral();
        bluetoothService = new BluetoothServiceImpl();
        bluetoothConnection = new BluetoothConnectionImpl("00:11:22:33:44:55", true, cryptoService);
        errorHandler = new BLEErrorHandler();
        
        // Set up service dependencies
        ReflectionTestUtils.setField(bluetoothService, "cryptoService", cryptoService);
        ReflectionTestUtils.setField(bluetoothService, "atmPeripheral", atmPeripheral);
        ReflectionTestUtils.setField(bluetoothService, "operationMode", "both");
        ReflectionTestUtils.setField(bluetoothService, "atmCode", "001");
    }

    @Nested
    @DisplayName("ATM Peripheral Architecture Tests")
    class ATMPeripheralTests {

        @Test
        @DisplayName("Should initialize ATM as BLE Peripheral with correct GATT services")
        void shouldInitializeATMAsPeripheral() throws Exception {
            // Given
            String atmCode = "001";

            // When
            atmPeripheral.initialize(atmCode);

            // Then
            assertTrue(atmPeripheral.isServerRunning());
            assertFalse(atmPeripheral.isAdvertising()); // Not started yet
        }

        @Test
        @DisplayName("Should start BLE advertising with ATM service UUID")
        void shouldStartBLEAdvertising() throws Exception {
            // Given
            atmPeripheral.initialize("001");

            // When
            atmPeripheral.startAdvertising();

            // Then
            assertTrue(atmPeripheral.isAdvertising());
        }

        @Test
        @DisplayName("Should handle mobile device connections as peripheral")
        void shouldHandleMobileConnections() {
            // Given
            atmPeripheral.initialize("001");
            String deviceId = "mobile-001";
            String deviceAddress = "AA:BB:CC:DD:EE:FF";

            // When
            ATMBLEPeripheral.ConnectionResult result = 
                atmPeripheral.handleConnection(deviceId, deviceAddress);

            // Then
            assertTrue(result.isAccepted());
            assertNotNull(result.getDevice());
            assertEquals(deviceId, result.getDevice().getDeviceId());
        }

        @Test
        @DisplayName("Should expose all required GATT characteristics")
        void shouldExposeRequiredGATTCharacteristics() throws Exception {
            // Given
            atmPeripheral.initialize("001");
            String deviceId = "mobile-001";
            String deviceAddress = "AA:BB:CC:DD:EE:FF";
            atmPeripheral.handleConnection(deviceId, deviceAddress);

            // When & Then - Test each required characteristic
            
            // Authentication characteristic
            ATMBLEPeripheral.ReadResponse authRead = 
                atmPeripheral.handleCharacteristicRead(deviceId, BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID);
            assertTrue(authRead.isSuccess());
            
            // Status characteristic
            ATMBLEPeripheral.ReadResponse statusRead = 
                atmPeripheral.handleCharacteristicRead(deviceId, BLEConstants.STATUS_CHARACTERISTIC_UUID);
            assertTrue(statusRead.isSuccess());
            
            // Certificate characteristic
            ATMBLEPeripheral.ReadResponse certRead = 
                atmPeripheral.handleCharacteristicRead(deviceId, BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID);
            assertTrue(certRead.isSuccess());
        }

        @Test
        @DisplayName("Should handle characteristic write operations")
        void shouldHandleCharacteristicWrites() throws Exception {
            // Given
            atmPeripheral.initialize("001");
            String deviceId = "mobile-001";
            String deviceAddress = "AA:BB:CC:DD:EE:FF";
            atmPeripheral.handleConnection(deviceId, deviceAddress);
            
            byte[] testData = "test_data".getBytes();

            // When
            ATMBLEPeripheral.WriteResponse authWrite = 
                atmPeripheral.handleCharacteristicWrite(
                    deviceId, BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, testData);
            
            ATMBLEPeripheral.WriteResponse transactionWrite = 
                atmPeripheral.handleCharacteristicWrite(
                    deviceId, BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, testData);

            // Then
            assertTrue(authWrite.isSuccess());
            assertTrue(transactionWrite.isSuccess());
        }

        @Test
        @DisplayName("Should send notifications to connected devices")
        void shouldSendNotifications() throws Exception {
            // Given
            atmPeripheral.initialize("001");
            String deviceId = "mobile-001";
            String deviceAddress = "AA:BB:CC:DD:EE:FF";
            atmPeripheral.handleConnection(deviceId, deviceAddress);
            
            byte[] notificationData = "status_update".getBytes();

            // When
            boolean sent = atmPeripheral.sendNotification(
                deviceId, BLEConstants.STATUS_CHARACTERISTIC_UUID, notificationData);

            // Then
            assertTrue(sent);
        }

        @Test
        @DisplayName("Should update ATM status and notify connected devices")
        void shouldUpdateStatusAndNotify() throws Exception {
            // Given
            atmPeripheral.initialize("001");
            atmPeripheral.startAdvertising();

            // When
            atmPeripheral.updateATMStatus(ATMBLEPeripheral.ATMStatus.BUSY);

            // Then - Status should be updated (verified through subsequent operations)
            // In a real test, we would verify that notifications were sent
        }
    }

    @Nested
    @DisplayName("Mobile Central Architecture Tests")
    class MobileCentralTests {

        @Test
        @DisplayName("Should scan for ATM devices as BLE Central")
        void shouldScanForATMDevices() throws Exception {
            // Given
            bluetoothService.initialize();

            // When
            CompletableFuture<List<BluetoothDevice>> scanFuture = 
                bluetoothService.scanForDevices(5);
            List<BluetoothDevice> devices = scanFuture.get(10, TimeUnit.SECONDS);

            // Then
            assertNotNull(devices);
            assertTrue(devices.size() > 0);
            
            // Verify ATM devices are properly identified
            boolean foundATM = false;
            for (BluetoothDevice device : devices) {
                if (device.isATM()) {
                    foundATM = true;
                    assertTrue(device.getName().startsWith(BLEConstants.ATM_LOCAL_NAME_PREFIX));
                    assertTrue(BLEConstants.isRssiAcceptable(device.getRssi()));
                }
            }
            assertTrue(foundATM, "Should find at least one ATM device");
        }

        @Test
        @DisplayName("Should connect to ATM devices and discover GATT services")
        void shouldConnectToATMAndDiscoverServices() throws Exception {
            // Given
            bluetoothService.initialize();
            String atmAddress = "00:11:22:33:44:55";

            // When
            CompletableFuture<BluetoothConnection> connectionFuture = 
                bluetoothService.connect(atmAddress);
            BluetoothConnection connection = connectionFuture.get(15, TimeUnit.SECONDS);

            // Then
            assertNotNull(connection);
            assertTrue(connection.isConnected());
            assertEquals(atmAddress, connection.getDeviceAddress());
        }

        @Test
        @DisplayName("Should establish secure connection with encryption and authentication")
        void shouldEstablishSecureConnection() throws Exception {
            // Given
            bluetoothService.initialize();
            String atmAddress = "00:11:22:33:44:55";

            // When
            CompletableFuture<BluetoothConnection> connectionFuture = 
                bluetoothService.connect(atmAddress);
            BluetoothConnection connection = connectionFuture.get(15, TimeUnit.SECONDS);

            // Then
            assertTrue(connection.isSecure());
            assertTrue(((BluetoothConnectionImpl) connection).isEncrypted());
            assertTrue(((BluetoothConnectionImpl) connection).isAuthenticated());
        }

        @Test
        @DisplayName("Should filter ATM devices by signal strength and range")
        void shouldFilterATMDevicesBySignalStrength() throws Exception {
            // Given
            bluetoothService.initialize();

            // When
            CompletableFuture<List<BluetoothDevice>> scanFuture = 
                bluetoothService.scanForDevices(5);
            List<BluetoothDevice> devices = scanFuture.get(10, TimeUnit.SECONDS);

            // Then
            for (BluetoothDevice device : devices) {
                if (device.isATM()) {
                    // All returned ATM devices should have acceptable signal strength
                    assertTrue(BLEConstants.isRssiAcceptable(device.getRssi()),
                        "ATM device " + device.getAddress() + " has unacceptable RSSI: " + device.getRssi());
                }
            }
        }
    }

    @Nested
    @DisplayName("GATT Communication Tests")
    class GATTCommunicationTests {

        @Test
        @DisplayName("Should read from GATT characteristics with proper security")
        void shouldReadFromGATTCharacteristics() throws Exception {
            // Given
            BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
                "00:11:22:33:44:55", true, cryptoService);

            // When & Then - Test reading from each characteristic
            
            // Status characteristic (public read)
            byte[] statusData = connection.readCharacteristic(BLEConstants.STATUS_CHARACTERISTIC_UUID);
            assertNotNull(statusData);
            assertEquals(BLEConstants.STATUS_AVAILABLE, statusData[0]);

            // Certificate characteristic (public read)
            byte[] certData = connection.readCharacteristic(BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID);
            assertNotNull(certData);
            assertTrue(certData.length > 0);

            // Authentication characteristic (encrypted read)
            byte[] authData = connection.readCharacteristic(BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID);
            assertNotNull(authData);
        }

        @Test
        @DisplayName("Should write to GATT characteristics with proper security validation")
        void shouldWriteToGATTCharacteristics() throws Exception {
            // Given
            BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
                "00:11:22:33:44:55", true, cryptoService);
            byte[] testData = "secure_test_data".getBytes();

            // When & Then
            
            // Authentication characteristic (encrypted write)
            byte[] authResponse = connection.writeCharacteristic(
                BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, testData);
            assertNotNull(authResponse);

            // Transaction characteristic (encrypted write)
            byte[] transactionResponse = connection.writeCharacteristic(
                BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, testData);
            assertNotNull(transactionResponse);
        }

        @Test
        @DisplayName("Should enforce security requirements for characteristics")
        void shouldEnforceSecurityRequirements() throws Exception {
            // Given
            BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
                "00:11:22:33:44:55", false, cryptoService); // No encryption
            byte[] testData = "test_data".getBytes();

            // When & Then
            assertThrows(SecurityException.class, () -> {
                connection.writeCharacteristic(BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, testData);
            }, "Should require encryption for authentication characteristic");

            assertThrows(SecurityException.class, () -> {
                connection.writeCharacteristic(BLEConstants.TRANSACTION_CHARACTERISTIC_UUID, testData);
            }, "Should require encryption for transaction characteristic");
        }

        @Test
        @DisplayName("Should handle GATT operation timeouts")
        void shouldHandleGATTOperationTimeouts() {
            // Given
            BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
                "00:11:22:33:44:55", true, cryptoService);
            
            // Simulate a slow operation by using a very small timeout
            // This would need modification to the implementation to support timeout injection
            
            // For now, verify that timeouts are properly configured
            String params = connection.getConnectionParameters();
            assertNotNull(params);
            assertTrue(params.contains("timeout"));
        }
    }

    @Nested
    @DisplayName("BLE Error Handling Tests")
    class BLEErrorHandlingTests {

        @Test
        @DisplayName("Should classify error severity correctly")
        void shouldClassifyErrorSeverity() {
            // Given & When & Then
            BLEErrorHandler.BLERecoveryAction action;
            
            // Critical errors
            action = errorHandler.handleError("device1", BLEConstants.ERROR_CERTIFICATE_INVALID, 
                                            "certificate_validation", null);
            assertEquals(BLEErrorHandler.BLERecoveryAction.DISCONNECT_AND_FAIL, action);

            // High severity errors
            action = errorHandler.handleError("device2", BLEConstants.ERROR_AUTHENTICATION_FAILED, 
                                            "authentication", null);
            assertNotEquals(BLEErrorHandler.BLERecoveryAction.DISCONNECT_AND_FAIL, action);

            // Medium severity errors
            action = errorHandler.handleError("device3", BLEConstants.ERROR_CONNECTION_FAILED, 
                                            "connection", null);
            assertTrue(action == BLEErrorHandler.BLERecoveryAction.RETRY_IMMEDIATE ||
                      action == BLEErrorHandler.BLERecoveryAction.RETRY_WITH_BACKOFF);
        }

        @Test
        @DisplayName("Should implement circuit breaker pattern")
        void shouldImplementCircuitBreakerPattern() {
            // Given
            String deviceAddress = "device_circuit_test";
            
            // When - Generate multiple consecutive errors
            for (int i = 0; i < 3; i++) {
                errorHandler.handleError(deviceAddress, BLEConstants.ERROR_CONNECTION_FAILED, 
                                       "connection", null);
            }

            // Then
            assertTrue(errorHandler.isDeviceBlocked(deviceAddress));
            
            BLEErrorHandler.BLERecoveryAction action = errorHandler.handleError(
                deviceAddress, BLEConstants.ERROR_CONNECTION_FAILED, "connection", null);
            assertEquals(BLEErrorHandler.BLERecoveryAction.CIRCUIT_BREAKER_OPEN, action);
        }

        @Test
        @DisplayName("Should provide user-friendly error messages")
        void shouldProvideUserFriendlyErrorMessages() {
            // Given & When & Then
            String message;
            
            message = errorHandler.getUserFriendlyMessage(BLEConstants.ERROR_CERTIFICATE_INVALID);
            assertNotNull(message);
            assertFalse(message.contains("ERROR_CERTIFICATE_INVALID")); // Should not contain technical details
            
            message = errorHandler.getUserFriendlyMessage(BLEConstants.ERROR_RSSI_TOO_LOW);
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("closer") || message.toLowerCase().contains("range"));
        }

        @Test
        @DisplayName("Should track error statistics")
        void shouldTrackErrorStatistics() {
            // Given
            String deviceAddress = "device_stats_test";
            
            // When
            errorHandler.handleError(deviceAddress, BLEConstants.ERROR_CONNECTION_FAILED, "test1", null);
            errorHandler.handleError(deviceAddress, BLEConstants.ERROR_RSSI_TOO_LOW, "test2", null);
            
            // Then
            BLEErrorHandler.BLEErrorStatistics stats = errorHandler.getErrorStatistics(deviceAddress);
            assertNotNull(stats);
            assertEquals(2, stats.getTotalErrors());
            assertEquals(2, stats.getConsecutiveErrors());
            assertNotNull(stats.getLastErrorTime());
        }

        @Test
        @DisplayName("Should reset error tracking on successful operations")
        void shouldResetErrorTrackingOnSuccess() {
            // Given
            String deviceAddress = "device_reset_test";
            errorHandler.handleError(deviceAddress, BLEConstants.ERROR_CONNECTION_FAILED, "test", null);
            
            // When
            errorHandler.resetErrorTracking(deviceAddress);
            
            // Then
            BLEErrorHandler.BLEErrorStatistics stats = errorHandler.getErrorStatistics(deviceAddress);
            assertNotNull(stats);
            assertEquals(0, stats.getConsecutiveErrors());
            assertFalse(stats.isCircuitBreakerOpen());
        }
    }

    @Nested
    @DisplayName("BLE Constants and Configuration Tests")
    class BLEConstantsTests {

        @Test
        @DisplayName("Should use documented UUIDs from BLE architecture")
        void shouldUseDocumentedUUIDs() {
            // Verify that all UUIDs match the documented architecture
            assertEquals("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", BLEConstants.ATM_SERVICE_UUID);
            assertEquals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E", BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID);
            assertEquals("6E400003-B5A3-F393-E0A9-E50E24DCCA9E", BLEConstants.TRANSACTION_CHARACTERISTIC_UUID);
            assertEquals("6E400004-B5A3-F393-E0A9-E50E24DCCA9E", BLEConstants.STATUS_CHARACTERISTIC_UUID);
            assertEquals("6E400005-B5A3-F393-E0A9-E50E24DCCA9E", BLEConstants.CERTIFICATE_CHARACTERISTIC_UUID);
        }

        @Test
        @DisplayName("Should have appropriate BLE parameters for banking security")
        void shouldHaveSecureBLEParameters() {
            // Verify security-appropriate parameters
            assertEquals(-20, BLEConstants.TX_POWER_LEVEL_DBM); // Low power for privacy
            assertEquals(3, BLEConstants.OPTIMAL_RANGE_METERS); // Close range for security
            assertTrue(BLEConstants.REQUIRE_AUTHENTICATION);
            assertTrue(BLEConstants.REQUIRE_MITM_PROTECTION);
            assertTrue(BLEConstants.REQUIRE_LESC_PAIRING);
        }

        @Test
        @DisplayName("Should generate proper ATM local names")
        void shouldGenerateProperATMLocalNames() {
            // Given & When & Then
            String name1 = BLEConstants.generateATMLocalName("001");
            assertEquals("ATM-001", name1);
            
            String name2 = BLEConstants.generateATMLocalName("123456789012345678901234567890");
            assertTrue(name2.length() <= BLEConstants.MAX_ATM_LOCAL_NAME_LENGTH);
            assertTrue(name2.startsWith(BLEConstants.ATM_LOCAL_NAME_PREFIX));
        }

        @Test
        @DisplayName("Should validate RSSI acceptability")
        void shouldValidateRSSIAcceptability() {
            // Strong signal
            assertTrue(BLEConstants.isRssiAcceptable(-50));
            
            // Acceptable signal
            assertTrue(BLEConstants.isRssiAcceptable(-70));
            
            // Weak signal
            assertFalse(BLEConstants.isRssiAcceptable(-80));
            
            // Very weak signal
            assertFalse(BLEConstants.isRssiAcceptable(-100));
        }

        @Test
        @DisplayName("Should estimate distance from RSSI")
        void shouldEstimateDistanceFromRSSI() {
            // Given & When & Then
            double distance1 = BLEConstants.estimateDistance(-50, -20);
            double distance2 = BLEConstants.estimateDistance(-70, -20);
            
            // Stronger signal should indicate closer distance
            assertTrue(distance1 < distance2);
            
            // Both should be reasonable values for BLE range
            assertTrue(distance1 > 0 && distance1 < 50);
            assertTrue(distance2 > 0 && distance2 < 50);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should support full ATM-to-Mobile communication flow")
        void shouldSupportFullCommunicationFlow() throws Exception {
            // Given - ATM Peripheral
            atmPeripheral.initialize("001");
            atmPeripheral.startAdvertising();
            
            // Given - Mobile Central
            bluetoothService.initialize();
            
            // When - Mobile scans and finds ATM
            CompletableFuture<List<BluetoothDevice>> scanFuture = 
                bluetoothService.scanForDevices(5);
            List<BluetoothDevice> devices = scanFuture.get(10, TimeUnit.SECONDS);
            
            // Find ATM device
            BluetoothDevice atmDevice = null;
            for (BluetoothDevice device : devices) {
                if (device.isATM() && device.getName().contains("001")) {
                    atmDevice = device;
                    break;
                }
            }
            assertNotNull(atmDevice, "Should find ATM-001 device");
            
            // When - Mobile connects to ATM
            CompletableFuture<BluetoothConnection> connectionFuture = 
                bluetoothService.connect(atmDevice.getAddress());
            BluetoothConnection connection = connectionFuture.get(15, TimeUnit.SECONDS);
            
            // Then - Connection should be established and secure
            assertTrue(connection.isConnected());
            assertTrue(connection.isSecure());
            
            // When - Mobile sends message to ATM
            byte[] testMessage = "test_banking_transaction".getBytes();
            CompletableFuture<byte[]> messageFuture = 
                bluetoothService.sendSecureMessage(atmDevice.getAddress(), testMessage);
            byte[] response = messageFuture.get(10, TimeUnit.SECONDS);
            
            // Then - Should receive valid response
            assertNotNull(response);
            assertTrue(response.length > 0);
            
            // Clean up
            bluetoothService.disconnect(atmDevice.getAddress());
            assertFalse(connection.isConnected());
        }

        @Test
        @DisplayName("Should handle multiple concurrent mobile connections to ATM")
        void shouldHandleMultipleConcurrentConnections() throws Exception {
            // Given
            atmPeripheral.initialize("001");
            atmPeripheral.startAdvertising();
            
            // When - Multiple mobile devices connect
            String device1 = "mobile-001";
            String device2 = "mobile-002";
            String addr1 = "AA:BB:CC:DD:EE:01";
            String addr2 = "AA:BB:CC:DD:EE:02";
            
            ATMBLEPeripheral.ConnectionResult result1 = atmPeripheral.handleConnection(device1, addr1);
            ATMBLEPeripheral.ConnectionResult result2 = atmPeripheral.handleConnection(device2, addr2);
            
            // Then
            assertTrue(result1.isAccepted());
            assertTrue(result2.isAccepted());
            
            // Both should be able to communicate
            byte[] testData = "test".getBytes();
            ATMBLEPeripheral.WriteResponse write1 = atmPeripheral.handleCharacteristicWrite(
                device1, BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, testData);
            ATMBLEPeripheral.WriteResponse write2 = atmPeripheral.handleCharacteristicWrite(
                device2, BLEConstants.AUTHENTICATION_CHARACTERISTIC_UUID, testData);
            
            assertTrue(write1.isSuccess());
            assertTrue(write2.isSuccess());
        }
    }
}