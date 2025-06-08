package com.atmconnect.domain.ports.outbound;

import java.util.concurrent.CompletableFuture;

/**
 * Port interface for sending notifications to customers.
 * Abstracts the notification delivery mechanism to allow for different
 * implementations (SMS, email, push notifications, etc.).
 */
public interface NotificationService {
    
    /**
     * Sends an OTP code via SMS to the specified phone number.
     *
     * @param phoneNumber the recipient's phone number
     * @param otpCode the one-time password code
     * @param transactionReference the transaction reference for tracking
     * @return CompletableFuture that completes when SMS is sent
     */
    CompletableFuture<NotificationResult> sendOtpSms(String phoneNumber, String otpCode, String transactionReference);
    
    /**
     * Sends a transaction confirmation via SMS.
     *
     * @param phoneNumber the recipient's phone number
     * @param transactionDetails the transaction details
     * @return CompletableFuture that completes when SMS is sent
     */
    CompletableFuture<NotificationResult> sendTransactionConfirmationSms(String phoneNumber, String transactionDetails);
    
    /**
     * Sends a security alert via SMS.
     *
     * @param phoneNumber the recipient's phone number
     * @param alertMessage the security alert message
     * @return CompletableFuture that completes when SMS is sent
     */
    CompletableFuture<NotificationResult> sendSecurityAlertSms(String phoneNumber, String alertMessage);
    
    /**
     * Sends an email notification.
     *
     * @param email the recipient's email address
     * @param subject the email subject
     * @param message the email message
     * @return CompletableFuture that completes when email is sent
     */
    CompletableFuture<NotificationResult> sendEmail(String email, String subject, String message);
    
    /**
     * Sends a push notification to a registered device.
     *
     * @param deviceId the target device identifier
     * @param title the notification title
     * @param message the notification message
     * @return CompletableFuture that completes when notification is sent
     */
    CompletableFuture<NotificationResult> sendPushNotification(String deviceId, String title, String message);
    
    /**
     * Result object for notification operations.
     */
    class NotificationResult {
        private final boolean successful;
        private final String messageId;
        private final String errorMessage;
        
        private NotificationResult(boolean successful, String messageId, String errorMessage) {
            this.successful = successful;
            this.messageId = messageId;
            this.errorMessage = errorMessage;
        }
        
        public static NotificationResult success(String messageId) {
            return new NotificationResult(true, messageId, null);
        }
        
        public static NotificationResult failure(String errorMessage) {
            return new NotificationResult(false, null, errorMessage);
        }
        
        public boolean isSuccessful() {
            return successful;
        }
        
        public String getMessageId() {
            return messageId;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}