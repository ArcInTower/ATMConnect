package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.ports.outbound.BluetoothService.BluetoothDevice;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BluetoothDeviceImpl implements BluetoothDevice {
    private final String address;
    private final String name;
    private final int rssi;
    private final boolean isATM;
    
    @Override
    public String getAddress() {
        return address;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getRssi() {
        return rssi;
    }
    
    @Override
    public boolean isATM() {
        return isATM && name != null && name.startsWith("ATM-");
    }
}