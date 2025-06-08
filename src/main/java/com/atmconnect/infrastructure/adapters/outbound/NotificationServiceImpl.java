package com.atmconnect.infrastructure.adapters.outbound;

import com.atmconnect.domain.ports.outbound.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of NotificationService that handles various notification channels.
 * In a production environment, this would integrate with actual SMS providers,
 * email services, and push notification systems.
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    @Override
    public CompletableFuture<NotificationResult> sendOtpSms(String phoneNumber, String otpCode, String transactionReference) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validatePhoneNumber(phoneNumber);
                validateOtpCode(otpCode);
                
                String maskedPhone = maskPhoneNumber(phoneNumber);
                log.info("Sending OTP SMS to {} for transaction: {}", maskedPhone, transactionReference);
                
                // TODO: Integrate with actual SMS service provider
                // Example: twilioClient.messages.create(phoneNumber, "Your ATM OTP: " + otpCode);
                
                // Simulate SMS sending delay
                Thread.sleep(500);
                
                String messageId = UUID.randomUUID().toString();
                log.info("OTP SMS sent successfully to {} with message ID: {}", maskedPhone, messageId);
                
                return NotificationResult.success(messageId);
                
            } catch (IllegalArgumentException e) {
                log.error("Invalid parameters for OTP SMS: {}", e.getMessage());
                return NotificationResult.failure("Invalid phone number or OTP format");
            } catch (Exception e) {
                log.error("Failed to send OTP SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
                return NotificationResult.failure("SMS service temporarily unavailable");
            }
        });
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendTransactionConfirmationSms(String phoneNumber, String transactionDetails) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validatePhoneNumber(phoneNumber);
                
                String maskedPhone = maskPhoneNumber(phoneNumber);
                log.info("Sending transaction confirmation SMS to {}", maskedPhone);
                
                // TODO: Integrate with actual SMS service provider
                // Example: twilioClient.messages.create(phoneNumber, "Transaction completed: " + transactionDetails);
                
                Thread.sleep(500);
                
                String messageId = UUID.randomUUID().toString();
                log.info("Transaction confirmation SMS sent to {} with message ID: {}", maskedPhone, messageId);
                
                return NotificationResult.success(messageId);
                
            } catch (Exception e) {
                log.error("Failed to send confirmation SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage());
                return NotificationResult.failure("SMS service temporarily unavailable");
            }
        });
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendSecurityAlertSms(String phoneNumber, String alertMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validatePhoneNumber(phoneNumber);
                
                String maskedPhone = maskPhoneNumber(phoneNumber);
                log.warn("Sending security alert SMS to {}: {}", maskedPhone, alertMessage);
                
                // TODO: Integrate with actual SMS service provider
                // High priority SMS for security alerts
                
                Thread.sleep(300); // Faster delivery for security alerts
                
                String messageId = UUID.randomUUID().toString();
                log.info("Security alert SMS sent to {} with message ID: {}", maskedPhone, messageId);
                
                return NotificationResult.success(messageId);
                
            } catch (Exception e) {
                log.error("CRITICAL: Failed to send security alert SMS to {}: {}", 
                    maskPhoneNumber(phoneNumber), e.getMessage());
                return NotificationResult.failure("Failed to send security alert");
            }
        });
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendEmail(String email, String subject, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateEmailAddress(email);
                
                String maskedEmail = maskEmailAddress(email);
                log.info("Sending email to {} with subject: {}", maskedEmail, subject);
                
                // TODO: Integrate with actual email service provider
                // Example: sendGridClient.send(email, subject, message);
                
                Thread.sleep(1000);
                
                String messageId = UUID.randomUUID().toString();
                log.info("Email sent to {} with message ID: {}", maskedEmail, messageId);
                
                return NotificationResult.success(messageId);
                
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", maskEmailAddress(email), e.getMessage());
                return NotificationResult.failure("Email service temporarily unavailable");
            }
        });
    }
    
    @Override
    public CompletableFuture<NotificationResult> sendPushNotification(String deviceId, String title, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Device ID cannot be null or empty");
                }
                
                log.info("Sending push notification to device: {} with title: {}", 
                    maskDeviceId(deviceId), title);
                
                // TODO: Integrate with actual push notification service
                // Example: firebaseClient.send(deviceId, title, message);
                
                Thread.sleep(200);
                
                String messageId = UUID.randomUUID().toString();
                log.info("Push notification sent to device {} with message ID: {}", 
                    maskDeviceId(deviceId), messageId);
                
                return NotificationResult.success(messageId);
                
            } catch (Exception e) {
                log.error("Failed to send push notification to device {}: {}", 
                    maskDeviceId(deviceId), e.getMessage());
                return NotificationResult.failure("Push notification service temporarily unavailable");
            }
        });
    }
    
    /**
     * Validates phone number format.
     *
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if phone number is invalid
     */
    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    /**
     * Validates email address format.
     *
     * @param email the email address to validate
     * @throws IllegalArgumentException if email is invalid
     */
    private void validateEmailAddress(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address format");
        }
    }
    
    /**
     * Validates OTP code format.
     *
     * @param otpCode the OTP code to validate
     * @throws IllegalArgumentException if OTP is invalid
     */
    private void validateOtpCode(String otpCode) {
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP code cannot be null or empty");
        }
        
        if (!otpCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP code must be exactly 6 digits");
        }
    }
    
    /**
     * Masks phone number for logging (shows only last 4 digits).
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(phoneNumber.length() - 4) + phoneNumber.substring(phoneNumber.length() - 4);
    }
    
    /**
     * Masks email address for logging.
     *
     * @param email the email address to mask
     * @return masked email address
     */
    private String maskEmailAddress(String email) {
        if (email == null || !email.contains("@")) {
            return "****@****.***";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        
        String maskedLocal = localPart.length() > 2 ? 
            localPart.charAt(0) + "*".repeat(localPart.length() - 2) + localPart.charAt(localPart.length() - 1) :
            "**";
            
        return maskedLocal + "@" + domainPart;
    }
    
    /**
     * Masks device ID for logging.
     *
     * @param deviceId the device ID to mask
     * @return masked device ID
     */
    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() <= 8) {
            return "****-****";
        }
        return deviceId.substring(0, 4) + "-****-" + deviceId.substring(deviceId.length() - 4);
    }
}