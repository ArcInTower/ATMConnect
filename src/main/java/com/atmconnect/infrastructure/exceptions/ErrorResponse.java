package com.atmconnect.infrastructure.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String errorId;
    private int errorCode;
    private String message;
    private Map<String, String> details;
    private LocalDateTime timestamp;
    private String path;
}