package com.atmconnect.infrastructure.bluetooth;

import com.atmconnect.domain.constants.BLEConstants;
import com.atmconnect.domain.ports.outbound.BluetoothService.BluetoothDevice;
import lombok.Data;

/**
 * Implementation of BluetoothDevice with enhanced ATM-specific features.
 * 
 * <p>This implementation includes manufacturer data parsing for ATM devices
 * and proper validation according to the BLE architecture specifications.
 */
@Data
public class BluetoothDeviceImpl implements BluetoothDevice {
    private final String address;
    private final String name;
    private final int rssi;
    private final boolean connectable;
    private final byte[] manufacturerData;
    private final String[] serviceUuids;
    
    // Parsed ATM-specific data
    private final ATMInfo atmInfo;
    
    public BluetoothDeviceImpl(String address, String name, int rssi, boolean connectable) {
        this(address, name, rssi, connectable, null, null);
    }
    
    public BluetoothDeviceImpl(String address, String name, int rssi, boolean connectable, 
                             byte[] manufacturerData) {
        this(address, name, rssi, connectable, manufacturerData, null);
    }
    
    public BluetoothDeviceImpl(String address, String name, int rssi, boolean connectable,
                             byte[] manufacturerData, String[] serviceUuids) {
        this.address = address;
        this.name = name;
        this.rssi = rssi;
        this.connectable = connectable;
        this.manufacturerData = manufacturerData != null ? manufacturerData.clone() : null;
        this.serviceUuids = serviceUuids != null ? serviceUuids.clone() : null;
        this.atmInfo = parseATMInfo();
    }
    
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
        // Enhanced ATM detection
        return isATMByName() || isATMByService() || isATMByManufacturerData();
    }
    
    /**
     * Gets the manufacturer data if available.
     * 
     * @return copy of manufacturer data or null if not available
     */
    public byte[] getManufacturerData() {
        return manufacturerData != null ? manufacturerData.clone() : null;
    }
    
    /**
     * Gets the advertised service UUIDs.
     * 
     * @return copy of service UUIDs array or null if not available
     */
    public String[] getServiceUuids() {
        return serviceUuids != null ? serviceUuids.clone() : null;
    }
    
    /**
     * Checks if the device is connectable.
     * 
     * @return true if device accepts connections
     */
    public boolean isConnectable() {
        return connectable;
    }
    
    /**
     * Gets ATM-specific information parsed from advertising data.
     * 
     * @return ATM information or null if not an ATM device
     */
    public ATMInfo getAtmInfo() {
        return atmInfo;
    }
    
    /**
     * Estimates the distance to the device based on RSSI.
     * 
     * @return estimated distance in meters
     */
    public double getEstimatedDistance() {
        return BLEConstants.estimateDistance(rssi, BLEConstants.TX_POWER_LEVEL_DBM);
    }
    
    /**
     * Checks if the signal strength is acceptable for secure connection.
     * 
     * @return true if RSSI is within acceptable range
     */
    public boolean hasAcceptableSignalStrength() {
        return BLEConstants.isRssiAcceptable(rssi);
    }
    
    /**
     * Checks if the device is within the optimal range for banking operations.
     * 
     * @return true if device is within optimal range
     */
    public boolean isInOptimalRange() {
        double distance = getEstimatedDistance();
        return distance <= BLEConstants.OPTIMAL_RANGE_METERS;
    }
    
    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================
    
    /**
     * Checks if device is ATM based on name.
     */
    private boolean isATMByName() {
        return name != null && name.startsWith(BLEConstants.ATM_LOCAL_NAME_PREFIX);
    }
    
    /**
     * Checks if device is ATM based on advertised services.
     */
    private boolean isATMByService() {
        if (serviceUuids == null) {
            return false;
        }
        
        for (String uuid : serviceUuids) {
            if (BLEConstants.ATM_SERVICE_UUID.equalsIgnoreCase(uuid)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if device is ATM based on manufacturer data.
     */
    private boolean isATMByManufacturerData() {
        if (manufacturerData == null || manufacturerData.length < 2) {
            return false;
        }
        
        // Check company ID
        int companyId = (manufacturerData[1] << 8) | (manufacturerData[0] & 0xFF);
        return companyId == BLEConstants.COMPANY_ID;
    }
    
    /**
     * Parses ATM-specific information from advertising data.
     */
    private ATMInfo parseATMInfo() {
        if (!isATM()) {
            return null;
        }
        
        String atmCode = parseATMCode();
        ATMCapabilities capabilities = parseATMCapabilities();
        ATMStatus status = parseATMStatus();
        CashLevel cashLevel = parseCashLevel();
        
        return new ATMInfo(atmCode, capabilities, status, cashLevel);
    }
    
    /**
     * Extracts ATM code from device name.
     */
    private String parseATMCode() {
        if (name != null && name.startsWith(BLEConstants.ATM_LOCAL_NAME_PREFIX)) {
            return name.substring(BLEConstants.ATM_LOCAL_NAME_PREFIX.length());
        }
        return null;
    }
    
    /**
     * Parses ATM capabilities from manufacturer data.
     */
    private ATMCapabilities parseATMCapabilities() {
        if (manufacturerData == null || manufacturerData.length < 4) {
            return new ATMCapabilities(); // Default capabilities
        }
        
        byte capabilityByte = manufacturerData[3];
        return new ATMCapabilities(
            (capabilityByte & BLEConstants.CAPABILITY_CASH_WITHDRAWAL) != 0,
            (capabilityByte & BLEConstants.CAPABILITY_BALANCE_INQUIRY) != 0,
            (capabilityByte & BLEConstants.CAPABILITY_TRANSFER) != 0,
            (capabilityByte & BLEConstants.CAPABILITY_PIN_CHANGE) != 0
        );
    }
    
    /**
     * Parses ATM status from manufacturer data.
     */
    private ATMStatus parseATMStatus() {
        if (manufacturerData == null || manufacturerData.length < 5) {
            return ATMStatus.UNKNOWN;
        }
        
        byte statusByte = manufacturerData[4];
        switch (statusByte) {
            case BLEConstants.STATUS_AVAILABLE:
                return ATMStatus.AVAILABLE;
            case BLEConstants.STATUS_BUSY:
                return ATMStatus.BUSY;
            case BLEConstants.STATUS_OUT_OF_SERVICE:
                return ATMStatus.OUT_OF_SERVICE;
            case BLEConstants.STATUS_MAINTENANCE:
                return ATMStatus.MAINTENANCE;
            default:
                return ATMStatus.UNKNOWN;
        }
    }
    
    /**
     * Parses cash level from manufacturer data.
     */
    private CashLevel parseCashLevel() {
        if (manufacturerData == null || manufacturerData.length < 6) {
            return CashLevel.UNKNOWN;
        }
        
        byte cashByte = manufacturerData[5];
        switch (cashByte) {
            case BLEConstants.CASH_LEVEL_LOW:
                return CashLevel.LOW;
            case BLEConstants.CASH_LEVEL_MEDIUM:
                return CashLevel.MEDIUM;
            case BLEConstants.CASH_LEVEL_HIGH:
                return CashLevel.HIGH;
            default:
                return CashLevel.UNKNOWN;
        }
    }
    
    // ============================================================================
    // INNER CLASSES AND ENUMS
    // ============================================================================
    
    /**
     * ATM-specific information parsed from advertising data.
     */
    public static class ATMInfo {
        private final String atmCode;
        private final ATMCapabilities capabilities;
        private final ATMStatus status;
        private final CashLevel cashLevel;
        
        public ATMInfo(String atmCode, ATMCapabilities capabilities, 
                      ATMStatus status, CashLevel cashLevel) {
            this.atmCode = atmCode;
            this.capabilities = capabilities;
            this.status = status;
            this.cashLevel = cashLevel;
        }
        
        public String getAtmCode() { return atmCode; }
        public ATMCapabilities getCapabilities() { return capabilities; }
        public ATMStatus getStatus() { return status; }
        public CashLevel getCashLevel() { return cashLevel; }
        
        @Override
        public String toString() {
            return String.format("ATMInfo{code='%s', status=%s, cash=%s, capabilities=%s}",
                               atmCode, status, cashLevel, capabilities);
        }
    }
    
    /**
     * ATM capabilities parsed from manufacturer data.
     */
    public static class ATMCapabilities {
        private final boolean cashWithdrawal;
        private final boolean balanceInquiry;
        private final boolean transfer;
        private final boolean pinChange;
        
        public ATMCapabilities() {
            this(true, true, true, true); // Default all capabilities
        }
        
        public ATMCapabilities(boolean cashWithdrawal, boolean balanceInquiry,
                             boolean transfer, boolean pinChange) {
            this.cashWithdrawal = cashWithdrawal;
            this.balanceInquiry = balanceInquiry;
            this.transfer = transfer;
            this.pinChange = pinChange;
        }
        
        public boolean supportsCashWithdrawal() { return cashWithdrawal; }
        public boolean supportsBalanceInquiry() { return balanceInquiry; }
        public boolean supportsTransfer() { return transfer; }
        public boolean supportsPinChange() { return pinChange; }
        
        @Override
        public String toString() {
            return String.format("ATMCapabilities{withdrawal=%s, balance=%s, transfer=%s, pin=%s}",
                               cashWithdrawal, balanceInquiry, transfer, pinChange);
        }
    }
    
    /**
     * ATM operational status.
     */
    public enum ATMStatus {
        AVAILABLE,
        BUSY,
        OUT_OF_SERVICE,
        MAINTENANCE,
        UNKNOWN
    }
    
    /**
     * ATM cash level indicator.
     */
    public enum CashLevel {
        LOW,
        MEDIUM,
        HIGH,
        UNKNOWN
    }
    
    @Override
    public String toString() {
        return String.format("BluetoothDevice{address='%s', name='%s', rssi=%d, isATM=%s, atmInfo=%s}",
                           address, name, rssi, isATM(), atmInfo);
    }
}