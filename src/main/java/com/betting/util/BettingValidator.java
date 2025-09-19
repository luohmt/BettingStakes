package com.betting.util;

import com.betting.api.exception.BettingException;
import com.betting.core.service.SessionService;

/**
 * Business-focused validation utilities
 * Method names directly reflect betting business operations
 */
public class BettingValidator {

    /**
     * Maximum allowed stake amount to prevent overflow issues
     */
    private static final int MAX_STAKE_AMOUNT = 1_000_000; // 1 million

    private BettingValidator() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates a bet offer ID - must be a valid betting option
     * 
     * @param betOfferId the bet offer ID to validate
     * @throws BettingException if the bet offer ID is invalid
     */
    public static void betOffer(int betOfferId) {
        if (betOfferId <= 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_BET_OFFER_ID,
                "Invalid bet offer ID: " + betOfferId);
        }
    }
    
    /**
     * Validates a stake amount - must be a valid betting amount
     * 
     * @param stake the stake amount to validate
     * @throws BettingException if the stake amount is invalid
     */
    public static void stakeAmount(int stake) {
        if (stake < 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_STAKE_AMOUNT,
                "Invalid stake amount: " + stake);
        }
        if (stake > MAX_STAKE_AMOUNT) {
            throw new BettingException(BettingException.ErrorCode.INVALID_STAKE_AMOUNT,
                "Stake amount too large: " + stake);
        }
    }
    
    /**
     * Validates a customer ID - must be a valid customer
     * 
     * @param customerId the customer ID to validate
     * @throws BettingException if the customer ID is invalid
     */
    public static void customer(int customerId) {
        if (customerId <= 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_CUSTOMER_ID,
                "Invalid customer ID: " + customerId);
        }
    }
    
    /**
     * Validates session key and returns customer ID for betting operations
     * 
     * @param sessionKey the session key to validate
     * @param sessionService the session service to use for validation
     * @return the customer ID associated with the session
     * @throws BettingException if the session key is invalid or expired
     */
    public static int authenticateCustomer(String sessionKey, SessionService sessionService) {
        if (sessionKey == null || sessionKey.trim().isEmpty()) {
            throw new BettingException(BettingException.ErrorCode.MISSING_PARAMETER,
                "Session key is required");
        }
        
        if (sessionService == null) {
            throw new BettingException(BettingException.ErrorCode.INTERNAL_ERROR,
                "Session service is not available");
        }
        
        if (!sessionService.validateSession(sessionKey)) {
            throw new BettingException(BettingException.ErrorCode.INVALID_SESSION,
                "Invalid or expired session: " + sessionKey);
        }
        
        int customerId = sessionService.getCustomerId(sessionKey);
        if (customerId == 0) {
            throw new BettingException(BettingException.ErrorCode.SESSION_NOT_FOUND,
                "Customer not found for session: " + sessionKey);
        }
        
        return customerId;
    }
}
