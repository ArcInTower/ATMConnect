package com.atmconnect.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SecureJwtTokenProvider {
    
    private final SecretKey signingKey;
    private final SecretKey encryptionKey;
    private final long jwtExpirationMs;
    private final long refreshTokenExpirationMs;
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final Map<String, TokenMetadata> activeTokens = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    private final SecureRandom secureRandom = new SecureRandom();
    
    private static class TokenMetadata {
        final String customerId;
        final String deviceId;
        final long issuedAt;
        final String sessionId;
        
        TokenMetadata(String customerId, String deviceId, String sessionId) {
            this.customerId = customerId;
            this.deviceId = deviceId;
            this.sessionId = sessionId;
            this.issuedAt = Instant.now().getEpochSecond();
        }
    }
    
    public SecureJwtTokenProvider(@Value("${jwt.expiration-ms:900000}") long jwtExpirationMs,
                                 @Value("${jwt.refresh-expiration-ms:86400000}") long refreshTokenExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        
        // Generate cryptographically secure keys
        this.signingKey = generateSecureKey();
        this.encryptionKey = generateSecureKey();
        
        // Start cleanup task
        startCleanupTask();
        
        log.info("Secure JWT provider initialized with dynamic keys");
    }
    
    private SecretKey generateSecureKey() {
        byte[] keyBytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(keyBytes);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = Instant.now().getEpochSecond();
            
            // Clean expired blacklisted tokens
            blacklistedTokens.removeIf(token -> {
                try {
                    Claims claims = parseTokenClaims(token);
                    return claims.getExpiration().getTime() < now * 1000;
                } catch (Exception e) {
                    return true; // Remove invalid tokens
                }
            });
            
            // Clean expired active tokens
            activeTokens.entrySet().removeIf(entry -> 
                (now - entry.getValue().issuedAt) > (jwtExpirationMs / 1000));
                
        }, 1, 1, TimeUnit.HOURS);
    }
    
    public String generateToken(String customerId, String deviceId, String sessionId) {
        if (customerId == null || deviceId == null || sessionId == null) {
            throw new IllegalArgumentException("Required parameters cannot be null");
        }
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        String jti = UUID.randomUUID().toString(); // Unique token ID
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("sessionId", sessionId);
        claims.put("iat", now.getTime() / 1000);
        claims.put("jti", jti);
        claims.put("tokenType", "access");
        
        String token = Jwts.builder()
                .setSubject(customerId)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("atmconnect-auth")
                .setAudience("atmconnect-api")
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
        
        // Store token metadata
        activeTokens.put(jti, new TokenMetadata(customerId, deviceId, sessionId));
        
        log.info("Generated secure token for customer: {} on device: {}", customerId, deviceId);
        return token;
    }
    
    public String generateRefreshToken(String customerId, String deviceId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        String jti = UUID.randomUUID().toString();
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("deviceId", deviceId);
        claims.put("jti", jti);
        claims.put("tokenType", "refresh");
        
        return Jwts.builder()
                .setSubject(customerId)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer("atmconnect-auth")
                .setAudience("atmconnect-refresh")
                .signWith(encryptionKey, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            String jti = claims.get("jti", String.class);
            String tokenType = claims.get("tokenType", String.class);
            
            // Check if token is blacklisted
            if (blacklistedTokens.contains(jti)) {
                log.warn("Attempted use of blacklisted token: {}", jti);
                return false;
            }
            
            // Verify token type
            if (!"access".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                return false;
            }
            
            // Check if token is in active tokens
            if (!activeTokens.containsKey(jti)) {
                log.warn("Token not found in active tokens: {}", jti);
                return false;
            }
            
            // Verify issuer and audience
            if (!"atmconnect-auth".equals(claims.getIssuer()) || 
                !"atmconnect-api".equals(claims.getAudience())) {
                log.warn("Invalid token issuer or audience");
                return false;
            }
            
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Token expired: {}", e.getMessage());
            return false;
        } catch (SecurityException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }
    
    public String getCustomerIdFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims.getSubject();
    }
    
    public String getDeviceIdFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims.get("deviceId", String.class);
    }
    
    public String getSessionIdFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims.get("sessionId", String.class);
    }
    
    public void blacklistToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            String jti = claims.get("jti", String.class);
            
            if (jti != null) {
                blacklistedTokens.add(jti);
                activeTokens.remove(jti);
                log.info("Token blacklisted: {}", jti);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }
    
    public void blacklistAllTokensForCustomer(String customerId) {
        activeTokens.entrySet().removeIf(entry -> {
            if (customerId.equals(entry.getValue().customerId)) {
                blacklistedTokens.add(entry.getKey());
                return true;
            }
            return false;
        });
        
        log.info("All tokens blacklisted for customer: {}", customerId);
    }
    
    public void blacklistAllTokensForDevice(String deviceId) {
        activeTokens.entrySet().removeIf(entry -> {
            if (deviceId.equals(entry.getValue().deviceId)) {
                blacklistedTokens.add(entry.getKey());
                return true;
            }
            return false;
        });
        
        log.info("All tokens blacklisted for device: {}", deviceId);
    }
    
    public String refreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(encryptionKey)
                    .requireIssuer("atmconnect-auth")
                    .requireAudience("atmconnect-refresh")
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            
            String customerId = claims.getSubject();
            String deviceId = claims.get("deviceId", String.class);
            String tokenType = claims.get("tokenType", String.class);
            
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("Invalid refresh token type");
            }
            
            // Generate new session ID for security
            String newSessionId = UUID.randomUUID().toString();
            
            return generateToken(customerId, deviceId, newSessionId);
        } catch (Exception e) {
            log.warn("Refresh token validation failed: {}", e.getMessage());
            throw new SecurityException("Invalid refresh token");
        }
    }
    
    private Claims parseTokenClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    public int getActiveTokenCount() {
        return activeTokens.size();
    }
    
    public int getBlacklistedTokenCount() {
        return blacklistedTokens.size();
    }
}