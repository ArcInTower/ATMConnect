package com.atmconnect.domain.ports.outbound;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface CryptoService {
    KeyPair generateKeyPair();
    byte[] encrypt(byte[] data, PublicKey publicKey);
    byte[] decrypt(byte[] encryptedData);
    byte[] sign(byte[] data);
    boolean verify(byte[] data, byte[] signature, PublicKey publicKey);
    String generateOTP();
    boolean verifyOTP(String otp, String secret);
    X509Certificate verifyCertificate(String certificateData);
    byte[] generateSecureRandom(int length);
    String computeHash(byte[] data);
    byte[] performKeyExchange(PublicKey peerPublicKey);
}