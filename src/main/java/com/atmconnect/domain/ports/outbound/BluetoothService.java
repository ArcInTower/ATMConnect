package com.atmconnect.domain.ports.outbound;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BluetoothService {
    CompletableFuture<List<BluetoothDevice>> scanForDevices(int timeoutSeconds);
    CompletableFuture<BluetoothConnection> connect(String deviceAddress);
    void disconnect(String deviceAddress);
    boolean isConnected(String deviceAddress);
    CompletableFuture<byte[]> sendSecureMessage(String deviceAddress, byte[] message);
    
    interface BluetoothDevice {
        String getAddress();
        String getName();
        int getRssi();
        boolean isATM();
    }
    
    interface BluetoothConnection {
        String getDeviceAddress();
        boolean isSecure();
        boolean isConnected();
        void close();
    }
}