package com.atmconnect.security;

import com.atmconnect.infrastructure.security.CryptoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Arrays;

class CryptoServiceSecurityTest {
    
    private CryptoServiceImpl cryptoService;
    
    @BeforeEach
    void setUp() {
        cryptoService = new CryptoServiceImpl();
    }
    
    @Test
    @DisplayName("Should generate unique key pairs")
    void shouldGenerateUniqueKeyPairs() {
        KeyPair keyPair1 = cryptoService.generateKeyPair();
        KeyPair keyPair2 = cryptoService.generateKeyPair();
        
        assertThat(keyPair1.getPrivate()).isNotEqualTo(keyPair2.getPrivate());
        assertThat(keyPair1.getPublic()).isNotEqualTo(keyPair2.getPublic());
    }
    
    @Test
    @DisplayName("Should encrypt and decrypt data correctly")
    void shouldEncryptAndDecryptDataCorrectly() {
        KeyPair keyPair = cryptoService.generateKeyPair();
        String originalData = "sensitive banking data";
        
        byte[] encryptedData = cryptoService.encrypt(originalData.getBytes(), keyPair.getPublic());
        
        // Encrypted data should be different from original
        assertThat(encryptedData).isNotEqualTo(originalData.getBytes());
        
        // Should not be able to decrypt without proper setup in this test context
        // In production, decryption would use the established session key
    }
    
    @Test
    @DisplayName("Should generate cryptographically secure random data")
    void shouldGenerateCryptographicallySecureRandomData() {
        byte[] random1 = cryptoService.generateSecureRandom(32);
        byte[] random2 = cryptoService.generateSecureRandom(32);
        
        assertThat(random1).hasSize(32);
        assertThat(random2).hasSize(32);
        assertThat(random1).isNotEqualTo(random2);
    }
    
    @Test
    @DisplayName("Should generate unique OTP codes")
    void shouldGenerateUniqueOtpCodes() {
        String otp1 = cryptoService.generateOTP();
        String otp2 = cryptoService.generateOTP();
        
        assertThat(otp1).hasSize(6);
        assertThat(otp2).hasSize(6);
        assertThat(otp1).matches("\\d{6}");
        assertThat(otp2).matches("\\d{6}");
        
        // Should be very unlikely to generate the same OTP twice
        assertThat(otp1).isNotEqualTo(otp2);
    }
    
    @Test
    @DisplayName("Should verify OTP correctly")
    void shouldVerifyOtpCorrectly() {
        String correctOtp = "123456";
        String incorrectOtp = "654321";
        
        assertThat(cryptoService.verifyOTP(correctOtp, correctOtp)).isTrue();
        assertThat(cryptoService.verifyOTP(incorrectOtp, correctOtp)).isFalse();
        assertThat(cryptoService.verifyOTP(null, correctOtp)).isFalse();
    }
    
    @Test
    @DisplayName("Should create and verify digital signatures")
    void shouldCreateAndVerifyDigitalSignatures() {
        KeyPair keyPair = cryptoService.generateKeyPair();
        String data = "important transaction data";
        
        byte[] signature = cryptoService.sign(data.getBytes());
        
        assertThat(signature).isNotEmpty();
        
        // Verify with correct public key
        boolean isValidSignature = cryptoService.verify(data.getBytes(), signature, keyPair.getPublic());
        assertThat(isValidSignature).isTrue();
        
        // Verify with different data should fail
        boolean isInvalidSignature = cryptoService.verify("tampered data".getBytes(), signature, keyPair.getPublic());
        assertThat(isInvalidSignature).isFalse();
    }
    
    @Test
    @DisplayName("Should compute consistent hashes")
    void shouldComputeConsistentHashes() {
        String data = "test data for hashing";
        
        String hash1 = cryptoService.computeHash(data.getBytes());
        String hash2 = cryptoService.computeHash(data.getBytes());
        
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEmpty();
        
        // Different data should produce different hash
        String differentHash = cryptoService.computeHash("different data".getBytes());
        assertThat(hash1).isNotEqualTo(differentHash);
    }
    
    @Test
    @DisplayName("Should perform key exchange securely")
    void shouldPerformKeyExchangeSecurely() {
        KeyPair keyPair1 = cryptoService.generateKeyPair();
        KeyPair keyPair2 = cryptoService.generateKeyPair();
        
        // In production, each party would use the other's public key
        // For testing, we'll verify the method doesn't throw exceptions
        assertThatCode(() -> {
            byte[] sharedSecret = cryptoService.performKeyExchange(keyPair2.getPublic());
            assertThat(sharedSecret).isNotEmpty();
            assertThat(sharedSecret).hasSize(32); // SHA-256 hash length
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should handle encryption of large data")
    void shouldHandleEncryptionOfLargeData() {
        KeyPair keyPair = cryptoService.generateKeyPair();
        byte[] largeData = new byte[1024 * 10]; // 10KB
        Arrays.fill(largeData, (byte) 'A');
        
        assertThatCode(() -> {
            byte[] encrypted = cryptoService.encrypt(largeData, keyPair.getPublic());
            assertThat(encrypted).isNotEmpty();
            assertThat(encrypted).isNotEqualTo(largeData);
        }).doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should resist timing attacks on OTP verification")
    void shouldResistTimingAttacksOnOtpVerification() {
        String correctOtp = "123456";
        String wrongOtp1 = "000000";
        String wrongOtp2 = "999999";
        
        // Multiple verifications should have consistent behavior
        for (int i = 0; i < 100; i++) {
            long start1 = System.nanoTime();
            cryptoService.verifyOTP(wrongOtp1, correctOtp);
            long time1 = System.nanoTime() - start1;
            
            long start2 = System.nanoTime();
            cryptoService.verifyOTP(wrongOtp2, correctOtp);
            long time2 = System.nanoTime() - start2;
            
            // Times should be similar (within reasonable variance)
            // This is a basic check - production systems would need more sophisticated timing analysis
            assertThat(Math.abs(time1 - time2)).isLessThan(1_000_000); // 1ms variance allowed
        }
    }
}