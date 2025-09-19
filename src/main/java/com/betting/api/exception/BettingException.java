package com.betting.api.exception;

/**
 * Custom exception for betting application
 * 
 * This exception provides structured error handling with error codes
 * and HTTP status codes for better API responses.
 */
public class BettingException extends RuntimeException {
    
    private final ErrorCode errorCode;
    private final String details;
    
    public BettingException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public BettingException(ErrorCode errorCode, String details) {
        super(errorCode.getMessage() + (details != null ? ": " + details : ""));
        this.errorCode = errorCode;
        this.details = details;
    }
    
    public BettingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public String getDetails() {
        return details;
    }
    
    public int getHttpStatusCode() {
        return errorCode.getHttpStatusCode();
    }
    
    /**
     * Error codes for the betting application
     */
    public enum ErrorCode {
        // Session related errors
        INVALID_SESSION("Invalid session", 403),
        SESSION_EXPIRED("Session expired", 403),
        SESSION_NOT_FOUND("Session not found", 404),
        
        // Stake related errors
        INVALID_STAKE_AMOUNT("Invalid stake amount", 400),
        INVALID_BET_OFFER_ID("Invalid bet offer ID", 400),
        STAKE_TOO_LOW("Stake amount too low", 400),
        STAKE_TOO_HIGH("Stake amount too high", 400),
        
        // Customer related errors
        INVALID_CUSTOMER_ID("Invalid customer ID", 400),
        CUSTOMER_NOT_FOUND("Customer not found", 404),
        
        // System errors
        INTERNAL_ERROR("Internal server error", 500),
        SERVICE_UNAVAILABLE("Service temporarily unavailable", 503),
        RATE_LIMIT_EXCEEDED("Rate limit exceeded", 429),
        
        // Validation errors
        MISSING_PARAMETER("Missing required parameter", 400),
        INVALID_PARAMETER_FORMAT("Invalid parameter format", 400);
        
        private final String message;
        private final int httpStatusCode;
        
        ErrorCode(String message, int httpStatusCode) {
            this.message = message;
            this.httpStatusCode = httpStatusCode;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getHttpStatusCode() {
            return httpStatusCode;
        }
    }
}
