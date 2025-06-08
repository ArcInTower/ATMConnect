package com.atmconnect.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    
    private boolean success;
    private String error;
    private String message;
    private String path;
    private int status;
    private LocalDateTime timestamp;
    private String traceId;
    private List<ValidationError> validationErrors;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
    
    public static ApiErrorResponse of(String error, String message, String path, int status) {
        return ApiErrorResponse.builder()
            .success(false)
            .error(error)
            .message(message)
            .path(path)
            .status(status)
            .timestamp(LocalDateTime.now())
            .build();
    }
}