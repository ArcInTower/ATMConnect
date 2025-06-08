package com.atmconnect.domain.constants;

/**
 * Bluetooth Low Energy (BLE) constants for ATMConnect system.
 * 
 * <p>This class defines all UUIDs, configuration parameters, and constants
 * required for proper BLE communication between ATM peripherals and mobile
 * central devices according to the documented BLE architecture.</p>
 * 
 * <h3>Architecture Overview:</h3>
 * <ul>
 *   <li><strong>ATM = BLE Peripheral</strong> - Advertises services, accepts connections</li>
 *   <li><strong>Mobile = BLE Central</strong> - Scans for ATMs, initiates connections</li>
 * </ul>
 * 
 * @see /docs/BLE_ARCHITECTURE.md
 */
public final class BLEConstants {
    
    // =================================================================
    // GATT SERVICE AND CHARACTERISTIC UUIDs
    // =================================================================
    
    /**
     * Primary ATM service UUID - identifies ATM devices in BLE advertising.
     * This service contains all characteristics needed for ATM operations.
     */
    public static final String ATM_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    
    /**
     * Authentication characteristic UUID - handles customer authentication.
     * Properties: READ, WRITE, NOTIFY
     * Security: ENCRYPT | AUTHENTICATE | MITM_PROTECTION_REQUIRED
     */
    public static final String AUTHENTICATION_CHARACTERISTIC_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    
    /**
     * Transaction characteristic UUID - handles banking transactions.
     * Properties: WRITE, NOTIFY
     * Security: ENCRYPT | AUTHENTICATE | AUTHORIZE | LESC_PAIRING_REQUIRED
     */
    public static final String TRANSACTION_CHARACTERISTIC_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    
    /**
     * Status characteristic UUID - provides ATM status information.
     * Properties: READ, NOTIFY
     * Security: NONE (public information)
     */
    public static final String STATUS_CHARACTERISTIC_UUID = "6E400004-B5A3-F393-E0A9-E50E24DCCA9E";
    
    /**
     * Certificate characteristic UUID - provides ATM's X.509 certificate.
     * Properties: READ
     * Security: NONE (public certificate for verification)
     */
    public static final String CERTIFICATE_CHARACTERISTIC_UUID = "6E400005-B5A3-F393-E0A9-E50E24DCCA9E";
    
    // =================================================================
    // BLE ADVERTISING CONFIGURATION
    // =================================================================
    
    /**
     * ATM advertising power level in dBm.
     * -20 dBm provides ~2-3 meter range for security and privacy.
     */
    public static final int TX_POWER_LEVEL_DBM = -20;
    
    /**
     * ATM advertising interval in milliseconds.
     * 1000ms balances discoverability with power consumption.
     */
    public static final int ADVERTISING_INTERVAL_MS = 1000;
    
    /**
     * ATM local name prefix for BLE advertising.
     * Full name format: "ATM-{atmCode}" (e.g., "ATM-001")
     */
    public static final String ATM_LOCAL_NAME_PREFIX = "ATM-";
    
    /**
     * Maximum length for ATM local name in BLE advertising.
     */
    public static final int MAX_ATM_LOCAL_NAME_LENGTH = 20;
    
    // =================================================================
    // CONNECTION PARAMETERS
    // =================================================================
    
    /**
     * BLE connection interval in units of 1.25ms.
     * 24 units = 30ms intervals for responsive banking operations.
     */
    public static final int CONNECTION_INTERVAL_UNITS = 24;
    
    /**
     * BLE slave latency (number of connection events to skip).
     * 0 = no latency for critical banking operations.
     */
    public static final int SLAVE_LATENCY = 0;
    
    /**
     * BLE supervision timeout in units of 10ms.
     * 200 units = 2 second timeout for connection supervision.
     */
    public static final int SUPERVISION_TIMEOUT_UNITS = 200;
    
    /**
     * Maximum time to wait for BLE connection establishment.
     */
    public static final int CONNECTION_TIMEOUT_MS = 10000;  // 10 seconds
    
    /**
     * Maximum duration for a banking transaction over BLE.
     */
    public static final int TRANSACTION_TIMEOUT_MS = 300000;  // 5 minutes
    
    /**
     * Idle timeout before automatic disconnection.
     */
    public static final int IDLE_TIMEOUT_MS = 60000;  // 1 minute
    
    // =================================================================
    // SECURITY CONFIGURATION
    // =================================================================
    
    /**
     * Minimum encryption key size for BLE connections.
     */
    public static final int MIN_ENCRYPTION_KEY_SIZE = 128;  // AES-128 minimum
    
    /**
     * Require authenticated encryption for sensitive characteristics.
     */
    public static final boolean REQUIRE_AUTHENTICATION = true;
    
    /**
     * Require authorization for transaction operations.
     */
    public static final boolean REQUIRE_AUTHORIZATION = true;
    
    /**
     * Require Man-in-the-Middle (MITM) protection.
     */
    public static final boolean REQUIRE_MITM_PROTECTION = true;
    
    /**
     * Require LE Secure Connections (LESC) pairing for transactions.
     */
    public static final boolean REQUIRE_LESC_PAIRING = true;
    
    // =================================================================
    // SIGNAL STRENGTH AND RANGE
    // =================================================================
    
    /**
     * Minimum RSSI (Received Signal Strength Indicator) for stable connection.
     * -70 dBm ensures reliable communication within security range.
     */
    public static final int MIN_RSSI_DBM = -70;
    
    /**
     * Maximum communication range in meters for security.
     * 5 meters prevents accidental connections from distant devices.
     */
    public static final int MAX_RANGE_METERS = 5;
    
    /**
     * Optimal range in meters for user interaction.
     * 2-3 meters provides privacy and prevents eavesdropping.
     */
    public static final int OPTIMAL_RANGE_METERS = 3;
    
    // =================================================================
    // SCANNING CONFIGURATION
    // =================================================================
    
    /**
     * Mobile scan window in milliseconds.
     * 100ms active scanning window for device discovery.
     */
    public static final int SCAN_WINDOW_MS = 100;
    
    /**
     * Mobile scan interval in milliseconds.
     * 1000ms scan every second for balance between discovery and power.
     */
    public static final int SCAN_INTERVAL_MS = 1000;
    
    /**
     * Maximum scan duration before auto-stop.
     * 30 seconds prevents excessive battery drain.
     */
    public static final int MAX_SCAN_DURATION_MS = 30000;
    
    /**
     * Scan mode for balanced power consumption and latency.
     * Corresponds to Android's ScanSettings.SCAN_MODE_BALANCED.
     */
    public static final int SCAN_MODE_BALANCED = 1;
    
    // =================================================================
    // MANUFACTURER DATA CONFIGURATION
    // =================================================================
    
    /**
     * Company identifier for banking consortium.
     * This would be registered with Bluetooth SIG for production use.
     */
    public static final int COMPANY_ID = 0x004C;  // Placeholder - needs real registration
    
    /**
     * ATM type identifier in manufacturer data.
     */
    public static final byte ATM_TYPE_STANDARD = 0x01;
    
    /**
     * ATM capability flags in manufacturer data.
     */
    public static final byte CAPABILITY_CASH_WITHDRAWAL = 0x01;
    public static final byte CAPABILITY_BALANCE_INQUIRY = 0x02;
    public static final byte CAPABILITY_TRANSFER = 0x04;
    public static final byte CAPABILITY_PIN_CHANGE = 0x08;
    public static final byte CAPABILITY_ALL = (byte) 0x0F;
    
    /**
     * ATM status values in manufacturer data.
     */
    public static final byte STATUS_AVAILABLE = 0x01;
    public static final byte STATUS_BUSY = 0x02;
    public static final byte STATUS_OUT_OF_SERVICE = 0x03;
    public static final byte STATUS_MAINTENANCE = 0x04;
    
    /**
     * Cash level indicators in manufacturer data.
     */
    public static final byte CASH_LEVEL_LOW = 0x01;
    public static final byte CASH_LEVEL_MEDIUM = 0x02;
    public static final byte CASH_LEVEL_HIGH = 0x03;
    
    // =================================================================
    // ERROR CODES
    // =================================================================
    
    /**
     * BLE-specific error codes for standardized error handling.
     */
    public static final int ERROR_CONNECTION_FAILED = 0x0101;
    public static final int ERROR_AUTHENTICATION_FAILED = 0x0102;
    public static final int ERROR_CERTIFICATE_INVALID = 0x0103;
    public static final int ERROR_TRANSACTION_TIMEOUT = 0x0104;
    public static final int ERROR_GATT_SERVICE_NOT_FOUND = 0x0105;
    public static final int ERROR_CHARACTERISTIC_NOT_FOUND = 0x0106;
    public static final int ERROR_ENCRYPTION_FAILED = 0x0107;
    public static final int ERROR_RSSI_TOO_LOW = 0x0108;
    public static final int ERROR_RANGE_EXCEEDED = 0x0109;
    public static final int ERROR_ADVERTISING_FAILED = 0x010A;
    public static final int ERROR_PAIRING_FAILED = 0x010B;
    
    // =================================================================
    // CHARACTERISTIC PROPERTIES
    // =================================================================
    
    /**
     * GATT characteristic property bitmasks.
     * These match the Bluetooth specification values.
     */
    public static final int PROPERTY_READ = 0x02;
    public static final int PROPERTY_WRITE = 0x08;
    public static final int PROPERTY_NOTIFY = 0x10;
    public static final int PROPERTY_INDICATE = 0x20;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 0x04;
    
    /**
     * GATT characteristic permission bitmasks.
     */
    public static final int PERMISSION_READ = 0x01;
    public static final int PERMISSION_WRITE = 0x10;
    public static final int PERMISSION_READ_ENCRYPTED = 0x02;
    public static final int PERMISSION_WRITE_ENCRYPTED = 0x20;
    public static final int PERMISSION_READ_ENCRYPTED_MITM = 0x04;
    public static final int PERMISSION_WRITE_ENCRYPTED_MITM = 0x40;
    
    // =================================================================
    // UTILITY METHODS
    // =================================================================
    
    /**
     * Converts connection interval from units to milliseconds.
     * 
     * @param units connection interval in 1.25ms units
     * @return interval in milliseconds
     */
    public static double connectionIntervalToMs(int units) {
        return units * 1.25;
    }
    
    /**
     * Converts supervision timeout from units to milliseconds.
     * 
     * @param units supervision timeout in 10ms units
     * @return timeout in milliseconds
     */
    public static int supervisionTimeoutToMs(int units) {
        return units * 10;
    }
    
    /**
     * Checks if RSSI is within acceptable range for secure connection.
     * 
     * @param rssi signal strength in dBm
     * @return true if RSSI is acceptable
     */
    public static boolean isRssiAcceptable(int rssi) {
        return rssi >= MIN_RSSI_DBM;
    }
    
    /**
     * Estimates distance based on RSSI and TX power.
     * 
     * @param rssi received signal strength in dBm
     * @param txPower transmit power in dBm
     * @return estimated distance in meters
     */
    public static double estimateDistance(int rssi, int txPower) {
        if (rssi == 0) return -1.0;
        
        double ratio = (double) (txPower - rssi) / 20.0;
        return Math.pow(10, ratio);
    }
    
    /**
     * Generates ATM local name from ATM code.
     * 
     * @param atmCode the ATM identification code
     * @return formatted local name for BLE advertising
     */
    public static String generateATMLocalName(String atmCode) {
        String localName = ATM_LOCAL_NAME_PREFIX + atmCode;
        if (localName.length() > MAX_ATM_LOCAL_NAME_LENGTH) {
            localName = localName.substring(0, MAX_ATM_LOCAL_NAME_LENGTH);
        }
        return localName;
    }
    
    private BLEConstants() {
        // Utility class - prevent instantiation
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}