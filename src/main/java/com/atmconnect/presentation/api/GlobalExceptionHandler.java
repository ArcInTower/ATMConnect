package com.atmconnect.presentation.api;

import com.atmconnect.infrastructure.exceptions.ATMConnectException;
import com.atmconnect.presentation.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API endpoints.
 * 
 * Provides centralized error handling and consistent error responses
 * across all API endpoints with proper logging and security considerations.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("Validation error - TraceId: {} - Path: {}", traceId, request.getDescription(false));
        
        List<ApiErrorResponse.ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::mapFieldError)
            .collect(Collectors.toList());
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("VALIDATION_ERROR")
            .message("Request validation failed")
            .path(extractPath(request))
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .validationErrors(validationErrors)
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles custom application exceptions.
     */
    @ExceptionHandler(ATMConnectException.class)
    public ResponseEntity<ApiErrorResponse> handleATMConnectException(
            ATMConnectException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("ATMConnect error - TraceId: {} - Error: {} - Message: {}", 
                traceId, ex.getErrorCode(), ex.getMessage());
        
        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode().name());
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error(ex.getErrorCode().name())
            .message(ex.getMessage())
            .path(extractPath(request))
            .status(status.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handles security authentication errors.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("Authentication error - TraceId: {} - Path: {}", traceId, request.getDescription(false));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("AUTHENTICATION_FAILED")
            .message("Invalid credentials")
            .path(extractPath(request))
            .status(HttpStatus.UNAUTHORIZED.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handles access denied errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("Access denied - TraceId: {} - Path: {}", traceId, request.getDescription(false));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("ACCESS_DENIED")
            .message("Access denied")
            .path(extractPath(request))
            .status(HttpStatus.FORBIDDEN.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("Invalid argument - TraceId: {} - Message: {} - Path: {}", 
                traceId, ex.getMessage(), request.getDescription(false));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("INVALID_REQUEST")
            .message("Invalid request parameters")
            .path(extractPath(request))
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * Handles illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.warn("Invalid state - TraceId: {} - Message: {} - Path: {}", 
                traceId, ex.getMessage(), request.getDescription(false));
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("INVALID_STATE")
            .message(ex.getMessage())
            .path(extractPath(request))
            .status(HttpStatus.CONFLICT.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected error - TraceId: {} - Path: {}", traceId, request.getDescription(false), ex);
        
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
            .success(false)
            .error("INTERNAL_SERVER_ERROR")
            .message("An unexpected error occurred")
            .path(extractPath(request))
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .timestamp(LocalDateTime.now())
            .traceId(traceId)
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    private ApiErrorResponse.ValidationError mapFieldError(FieldError fieldError) {
        return ApiErrorResponse.ValidationError.builder()
            .field(fieldError.getField())
            .message(fieldError.getDefaultMessage())
            .rejectedValue(fieldError.getRejectedValue())
            .build();
    }
    
    private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
        switch (errorCode) {
            case "ACCOUNT_NOT_FOUND":
            case "CUSTOMER_NOT_FOUND":
            case "TRANSACTION_NOT_FOUND":
                return HttpStatus.NOT_FOUND;
            case "INSUFFICIENT_FUNDS":
            case "DAILY_LIMIT_EXCEEDED":
            case "ACCOUNT_LOCKED":
                return HttpStatus.FORBIDDEN;
            case "INVALID_PIN":
            case "AUTHENTICATION_FAILED":
                return HttpStatus.UNAUTHORIZED;
            case "ATM_NOT_AVAILABLE":
            case "SERVICE_UNAVAILABLE":
                return HttpStatus.SERVICE_UNAVAILABLE;
            default:
                return HttpStatus.BAD_REQUEST;
        }
    }
    
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        if (description.startsWith("uri=")) {
            return description.substring(4);
        }
        return description;
    }
}