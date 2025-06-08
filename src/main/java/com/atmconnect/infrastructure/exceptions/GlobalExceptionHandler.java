package com.atmconnect.infrastructure.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ATMConnectException.class)
    public ResponseEntity<ErrorResponse> handleATMConnectException(
            ATMConnectException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        log.error("ATMConnect exception occurred [ErrorId: {}]: {}", 
            errorId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ex.getErrorCode().getCode())
            .message(ex.getErrorCode().getUserMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, ex.getErrorCode().getHttpStatus());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error occurred [ErrorId: {}]: {}", errorId, validationErrors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
            .message("Validation failed")
            .details(validationErrors)
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        log.warn("Constraint violation [ErrorId: {}]: {}", errorId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ErrorCode.INVALID_FIELD_VALUE.getCode())
            .message("Invalid input provided")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        log.warn("Illegal argument [ErrorId: {}]: {}", errorId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ErrorCode.INVALID_FIELD_VALUE.getCode())
            .message("Invalid input provided")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        log.error("Illegal state [ErrorId: {}]: {}", errorId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .message("An unexpected error occurred")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString();
        
        log.error("Unexpected error occurred [ErrorId: {}]: {}", 
            errorId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorId(errorId)
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .message("An unexpected error occurred. Please try again later")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}