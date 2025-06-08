package com.atmconnect.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private SecureJwtTokenProvider tokenProvider;
    
    @Autowired
    private SecurityMonitorService securityMonitor;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String clientIP = getClientIP(request);
        
        // Check if IP is blocked
        if (securityMonitor.isIPBlocked(clientIP)) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Access Denied\",\"message\":\"IP blocked due to security violations\"}");
            return;
        }
        
        String token = extractTokenFromRequest(request);
        
        if (StringUtils.hasText(token)) {
            try {
                if (tokenProvider.validateToken(token)) {
                    String customerId = tokenProvider.getCustomerIdFromToken(token);
                    String deviceId = tokenProvider.getDeviceIdFromToken(token);
                    String sessionId = tokenProvider.getSessionIdFromToken(token);
                    
                    // Create enhanced principal with session info
                    EnhancedCustomerPrincipal principal = new EnhancedCustomerPrincipal(
                        customerId, deviceId, sessionId, clientIP);
                    
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                    );
                    
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Authentication successful for customer: {} from IP: {}", 
                        customerId, clientIP);
                } else {
                    securityMonitor.recordSecurityEvent(
                        SecurityMonitorService.SecurityEventType.TOKEN_MANIPULATION,
                        SecurityMonitorService.SecurityLevel.HIGH,
                        clientIP,
                        "Invalid token presented"
                    );
                }
            } catch (Exception e) {
                log.warn("Token validation failed from IP {}: {}", clientIP, e.getMessage());
                securityMonitor.recordSecurityEvent(
                    SecurityMonitorService.SecurityEventType.TOKEN_MANIPULATION,
                    SecurityMonitorService.SecurityLevel.MEDIUM,
                    clientIP,
                    "Token validation exception: " + e.getClass().getSimpleName()
                );
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            
            // Basic token format validation
            if (token.length() > 1000) {
                log.warn("Oversized token detected from IP: {}", getClientIP(request));
                return null;
            }
            
            return token;
        }
        return null;
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
}