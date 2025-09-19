package com.betting.util;

import com.betting.api.exception.BettingException;
import com.betting.core.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for BettingValidator
 * Tests all validation methods with various scenarios
 */
@DisplayName("BettingValidator Tests")
class BettingValidatorTest {

    private SessionService mockSessionService;

    @BeforeEach
    void setUp() {
        // Create a simple mock session service for testing
        mockSessionService = new SessionService() {
            @Override
            public com.betting.core.model.Session createOrGetSession(int customerId) {
                return new com.betting.core.model.Session(customerId, "valid-session-123",
                        System.currentTimeMillis() + 3600 * 1000);
            }

            @Override
            public boolean validateSession(String sessionKey) {
                return sessionKey != null && sessionKey.startsWith("valid-");
            }

            @Override
            public int getCustomerId(String sessionKey) {
                if (sessionKey.equals("valid-session-123")) {
                    return 123;
                } else if (sessionKey.equals("valid-session-456")) {
                    return 456;
                }
                return 0; // Session not found
            }
        };
    }

    @Test
    @DisplayName("betOffer - should pass for positive values")
    void testBetOffer_ValidValues() {
        // Test valid bet offer IDs
        assertDoesNotThrow(() -> BettingValidator.betOffer(1));
        assertDoesNotThrow(() -> BettingValidator.betOffer(100));
        assertDoesNotThrow(() -> BettingValidator.betOffer(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("betOffer - should throw exception for invalid values")
    void testBetOffer_InvalidValues() {
        // Test invalid bet offer IDs
        BettingException exception1 = assertThrows(BettingException.class, 
            () -> BettingValidator.betOffer(0));
        assertEquals(BettingException.ErrorCode.INVALID_BET_OFFER_ID, exception1.getErrorCode());
        assertTrue(exception1.getMessage().contains("Invalid bet offer ID: 0"));

        BettingException exception2 = assertThrows(BettingException.class, 
            () -> BettingValidator.betOffer(-1));
        assertEquals(BettingException.ErrorCode.INVALID_BET_OFFER_ID, exception2.getErrorCode());
        assertTrue(exception2.getMessage().contains("Invalid bet offer ID: -1"));

        BettingException exception3 = assertThrows(BettingException.class, 
            () -> BettingValidator.betOffer(-100));
        assertEquals(BettingException.ErrorCode.INVALID_BET_OFFER_ID, exception3.getErrorCode());
    }

    @Test
    @DisplayName("stakeAmount - should pass for non-negative values")
    void testStakeAmount_ValidValues() {
        // Test valid stake amounts
        assertDoesNotThrow(() -> BettingValidator.stakeAmount(0));
        assertDoesNotThrow(() -> BettingValidator.stakeAmount(1));
        assertDoesNotThrow(() -> BettingValidator.stakeAmount(1000));
    }

    @Test
    @DisplayName("stakeAmount - should throw exception for negative values")
    void testStakeAmount_InvalidValues() {
        // Test invalid stake amounts
        BettingException exception1 = assertThrows(BettingException.class, 
            () -> BettingValidator.stakeAmount(-1));
        assertEquals(BettingException.ErrorCode.INVALID_STAKE_AMOUNT, exception1.getErrorCode());
        assertTrue(exception1.getMessage().contains("Invalid stake amount: -1"));

        BettingException exception2 = assertThrows(BettingException.class, 
            () -> BettingValidator.stakeAmount(-100));
        assertEquals(BettingException.ErrorCode.INVALID_STAKE_AMOUNT, exception2.getErrorCode());
    }

    @Test
    @DisplayName("customer - should pass for positive values")
    void testCustomer_ValidValues() {
        // Test valid customer IDs
        assertDoesNotThrow(() -> BettingValidator.customer(1));
        assertDoesNotThrow(() -> BettingValidator.customer(100));
        assertDoesNotThrow(() -> BettingValidator.customer(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("customer - should throw exception for invalid values")
    void testCustomer_InvalidValues() {
        // Test invalid customer IDs
        BettingException exception1 = assertThrows(BettingException.class, 
            () -> BettingValidator.customer(0));
        assertEquals(BettingException.ErrorCode.INVALID_CUSTOMER_ID, exception1.getErrorCode());
        assertTrue(exception1.getMessage().contains("Invalid customer ID: 0"));

        BettingException exception2 = assertThrows(BettingException.class, 
            () -> BettingValidator.customer(-1));
        assertEquals(BettingException.ErrorCode.INVALID_CUSTOMER_ID, exception2.getErrorCode());
    }

    @Test
    @DisplayName("authenticateCustomer - should return customer ID for valid session")
    void testAuthenticateCustomer_ValidSession() {
        // Test valid session authentication
        int customerId = BettingValidator.authenticateCustomer("valid-session-123", mockSessionService);
        assertEquals(123, customerId);

        int customerId2 = BettingValidator.authenticateCustomer("valid-session-456", mockSessionService);
        assertEquals(456, customerId2);
    }

    @Test
    @DisplayName("authenticateCustomer - should throw exception for null session key")
    void testAuthenticateCustomer_NullSessionKey() {
        BettingException exception = assertThrows(BettingException.class, 
            () -> BettingValidator.authenticateCustomer(null, mockSessionService));
        assertEquals(BettingException.ErrorCode.MISSING_PARAMETER, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Session key is required"));
    }

    @Test
    @DisplayName("authenticateCustomer - should throw exception for empty session key")
    void testAuthenticateCustomer_EmptySessionKey() {
        BettingException exception1 = assertThrows(BettingException.class, 
            () -> BettingValidator.authenticateCustomer("", mockSessionService));
        assertEquals(BettingException.ErrorCode.MISSING_PARAMETER, exception1.getErrorCode());

        BettingException exception2 = assertThrows(BettingException.class, 
            () -> BettingValidator.authenticateCustomer("   ", mockSessionService));
        assertEquals(BettingException.ErrorCode.MISSING_PARAMETER, exception2.getErrorCode());
    }

    @Test
    @DisplayName("authenticateCustomer - should throw exception for invalid session")
    void testAuthenticateCustomer_InvalidSession() {
        BettingException exception = assertThrows(BettingException.class, 
            () -> BettingValidator.authenticateCustomer("invalid-session", mockSessionService));
        assertEquals(BettingException.ErrorCode.INVALID_SESSION, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Invalid or expired session: invalid-session"));
    }

    @Test
    @DisplayName("authenticateCustomer - should throw exception for session not found")
    void testAuthenticateCustomer_SessionNotFound() {
        BettingException exception = assertThrows(BettingException.class, 
            () -> BettingValidator.authenticateCustomer("valid-session-999", mockSessionService));
        assertEquals(BettingException.ErrorCode.SESSION_NOT_FOUND, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Customer not found for session: valid-session-999"));
    }

    @Test
    @DisplayName("Integration test - complete validation flow")
    void testCompleteValidationFlow() {
        // Test a complete validation flow that would be used in a controller
        int betOfferId = 123;
        int stake = 100;
        String sessionKey = "valid-session-456";

        // All validations should pass
        assertDoesNotThrow(() -> {
            BettingValidator.betOffer(betOfferId);
            BettingValidator.stakeAmount(stake);
            int customerId = BettingValidator.authenticateCustomer(sessionKey, mockSessionService);
            assertEquals(456, customerId);
        });
    }

    @Test
    @DisplayName("Edge cases - boundary values")
    void testBoundaryValues() {
        // Test boundary values
        assertDoesNotThrow(() -> BettingValidator.betOffer(1)); // Minimum valid bet offer
        assertDoesNotThrow(() -> BettingValidator.stakeAmount(0)); // Minimum valid stake
        assertDoesNotThrow(() -> BettingValidator.customer(1)); // Minimum valid customer

        // Test just over boundary
        assertThrows(BettingException.class, () -> BettingValidator.betOffer(0));
        assertThrows(BettingException.class, () -> BettingValidator.stakeAmount(-1));
        assertThrows(BettingException.class, () -> BettingValidator.customer(0));
    }
}
