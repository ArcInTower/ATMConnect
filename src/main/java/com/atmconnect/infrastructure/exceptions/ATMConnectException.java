package com.atmconnect.infrastructure.exceptions;

import lombok.Getter;

@Getter
public class ATMConnectException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String userMessage;
    
    public ATMConnectException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
    }
    
    public ATMConnectException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
    }
    
    public ATMConnectException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
    }
    
    public ATMConnectException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.userMessage = errorCode.getUserMessage();
    }
}