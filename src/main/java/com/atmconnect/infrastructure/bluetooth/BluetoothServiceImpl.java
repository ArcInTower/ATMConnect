package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.ports.outbound.BluetoothService;
import com.atmconnect.infrastructure.security.CryptoService;
import com.atmconnect.infrastructure.security.SecureMessageProtocol;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BluetoothServiceImpl implements BluetoothService {
    
    private final Map<String, BluetoothConnectionImpl> activeConnections = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    
    @Autowired
    private CryptoService cryptoService;
    
    private DBusConnection dbusConnection;
    
    public BluetoothServiceImpl() {
        initializeDBusConnection();
        startConnectionMonitor();
    }
    
    private void initializeDBusConnection() {
        try {
            dbusConnection = DBusConnection.getConnection(DBusConnection.DBusType.SYSTEM);
            log.info("DBus connection established for Bluetooth service");
        } catch (DBusException e) {
            log.error("Failed to initialize DBus connection", e);
        }
    }
    
    private void startConnectionMonitor() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            activeConnections.values().stream()
                .filter(conn -> !conn.isConnected())
                .forEach(conn -> {
                    activeConnections.remove(conn.getDeviceAddress());
                    log.info("Removed inactive connection: {}", conn.getDeviceAddress());
                });
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public CompletableFuture<List<BluetoothDevice>> scanForDevices(int timeoutSeconds) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Starting Bluetooth device scan for {} seconds", timeoutSeconds);
            List<BluetoothDevice> devices = new ArrayList<>();
            
            // Simulated scan for demonstration
            // In production, this would use actual Bluetooth APIs
            devices.add(new BluetoothDeviceImpl(
                "00:11:22:33:44:55",
                "ATM-001",
                -65,
                true
            ));
            
            devices.add(new BluetoothDeviceImpl(
                "00:11:22:33:44:66",
                "ATM-002",
                -72,
                true
            ));
            
            log.info("Scan completed. Found {} devices", devices.size());
            return devices;
        }, executorService);
    }
    
    @Override
    public CompletableFuture<BluetoothConnection> connect(String deviceAddress) {
        return CompletableFuture.supplyAsync(() -> {
            if (activeConnections.containsKey(deviceAddress)) {
                log.info("Reusing existing connection to {}", deviceAddress);
                return activeConnections.get(deviceAddress);
            }
            
            log.info("Establishing new connection to {}", deviceAddress);
            
            try {
                // Simulate connection establishment
                Thread.sleep(1000);
                
                BluetoothConnectionImpl connection = new BluetoothConnectionImpl(
                    deviceAddress,
                    true,
                    cryptoService
                );
                
                activeConnections.put(deviceAddress, connection);
                log.info("Successfully connected to {}", deviceAddress);
                
                return connection;
            } catch (Exception e) {
                log.error("Failed to connect to device {}", deviceAddress, e);
                throw new RuntimeException("Connection failed", e);
            }
        }, executorService);
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
    
    public void shutdown() {
        scheduledExecutor.shutdown();
        executorService.shutdown();
        activeConnections.values().forEach(BluetoothConnectionImpl::close);
        activeConnections.clear();
        
        if (dbusConnection != null) {
            try {
                dbusConnection.close();
            } catch (Exception e) {
                log.error("Error closing DBus connection", e);
            }
        }
    }
}