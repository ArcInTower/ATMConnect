package com.atmconnect.infrastructure.security;

import com.atmconnect.domain.ports.outbound.CryptoService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.Instant;
import java.io.ByteArrayInputStream;

@Service
public class CryptoServiceImpl implements CryptoService {
    private static final String KEY_ALGORITHM = "EC";
    private static final String CURVE_NAME = "secp256r1";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_AGREEMENT_ALGORITHM = "ECDH";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    private final KeyPair serviceKeyPair;
    private final SecureRandom secureRandom;
    private final Map<String, SessionKey> sessionKeys = new ConcurrentHashMap<>();
    
    private static class SessionKey {
        final byte[] key;
        final long timestamp;
        
        SessionKey(byte[] key) {
            this.key = key;
            this.timestamp = Instant.now().getEpochSecond();
        }
        
        boolean isExpired() {
            return (Instant.now().getEpochSecond() - timestamp) > 3600; // 1 hour
        }
    }
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    public CryptoServiceImpl() {
        this.secureRandom = new SecureRandom();
        this.serviceKeyPair = generateKeyPair();
        // Initialize default session key for basic operations
        byte[] defaultKey = new byte[32];
        secureRandom.nextBytes(defaultKey);
        sessionKeys.put("default", new SessionKey(defaultKey));
    }
    
    @Override
    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM, "BC");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE_NAME);
            keyGen.initialize(ecSpec, secureRandom);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }
    
    @Override
    public byte[] encrypt(byte[] data, PublicKey publicKey) {
        try {
            byte[] sharedSecret = performKeyExchange(publicKey);
            SecretKey secretKey = new SecretKeySpec(sharedSecret, 0, 32, "AES");
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            byte[] encryptedData = cipher.doFinal(data);
            
            byte[] result = new byte[iv.length + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    @Override
    public byte[] decrypt(byte[] encryptedData) {
        return decrypt(encryptedData, "default");
    }
    
    public byte[] decrypt(byte[] encryptedData, String sessionId) {
        try {
            if (encryptedData == null || encryptedData.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }
            
            SessionKey sessionKey = sessionKeys.get(sessionId);
            if (sessionKey == null || sessionKey.isExpired()) {
                throw new SecurityException("Invalid or expired session");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, iv.length);
            
            byte[] cipherText = new byte[encryptedData.length - iv.length];
            System.arraycopy(encryptedData, iv.length, cipherText, 0, cipherText.length);
            
            SecretKey secretKey = new SecretKeySpec(sessionKey.key, 0, 32, "AES");
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            return cipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    @Override
    public byte[] sign(byte[] data) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            signature.initSign(serviceKeyPair.getPrivate());
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }
    
    @Override
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM, "BC");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String generateOTP() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
    
    @Override
    public boolean verifyOTP(String otp, String secret) {
        if (otp == null || secret == null) {
            return false;
        }
        
        // Constant-time comparison to prevent timing attacks
        if (otp.length() != secret.length()) {
            return false;
        }
        
        return MessageDigest.isEqual(otp.getBytes(), secret.getBytes());
    }
    
    @Override
    public X509Certificate verifyCertificate(String certificateData) {
        try {
            byte[] certBytes = Base64.getDecoder().decode(certificateData);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(certBytes)
            );
            
            cert.checkValidity();
            
            return cert;
        } catch (Exception e) {
            throw new RuntimeException("Certificate verification failed", e);
        }
    }
    
    @Override
    public byte[] generateSecureRandom(int length) {
        byte[] random = new byte[length];
        secureRandom.nextBytes(random);
        return random;
    }
    
    @Override
    public String computeHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
    
    @Override
    public byte[] performKeyExchange(PublicKey peerPublicKey) {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM, "BC");
            keyAgreement.init(serviceKeyPair.getPrivate());
            keyAgreement.doPhase(peerPublicKey, true);
            
            byte[] sharedSecret = keyAgreement.generateSecret();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(sharedSecret);
        } catch (Exception e) {
            throw new RuntimeException("Key exchange failed", e);
        }
    }
}