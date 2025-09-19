package com.betting.core.service;

import com.betting.core.model.Session;
import com.betting.core.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionService interface
 * Tests core session management functionality
 */
@DisplayName("SessionService Tests")
class SessionServiceTest {

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl();
    }

    @Test
    @DisplayName("Should create new session for valid customer")
    void shouldCreateNewSessionForValidCustomer() {
        // Given
        int customerId = 123;

        // When
        Session session = sessionService.createOrGetSession(customerId);

        // Then
        assertNotNull(session);
        assertEquals(customerId, session.getCustomerId());
        assertNotNull(session.getSessionKey());
        assertFalse(session.getSessionKey().isEmpty());
        assertTrue(session.getExpiryTime() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("Should return existing session for same customer")
    void shouldReturnExistingSessionForSameCustomer() {
        // Given
        int customerId = 456;
        Session firstSession = sessionService.createOrGetSession(customerId);

        // When
        Session secondSession = sessionService.createOrGetSession(customerId);

        // Then
        assertEquals(firstSession.getSessionKey(), secondSession.getSessionKey());
        assertEquals(firstSession.getCustomerId(), secondSession.getCustomerId());
    }

    @Test
    @DisplayName("Should validate active session")
    void shouldValidateActiveSession() {
        // Given
        int customerId = 789;
        Session session = sessionService.createOrGetSession(customerId);

        // When
        boolean isValid = sessionService.validateSession(session.getSessionKey());

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid session")
    void shouldReturnFalseForInvalidSession() {
        // When
        boolean isValid = sessionService.validateSession("invalid-session-key");

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for null session")
    void shouldReturnFalseForNullSession() {
        // When
        boolean isValid = sessionService.validateSession(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should get customer ID for valid session")
    void shouldGetCustomerIdForValidSession() {
        // Given
        int customerId = 999;
        Session session = sessionService.createOrGetSession(customerId);

        // When
        int retrievedCustomerId = sessionService.getCustomerId(session.getSessionKey());

        // Then
        assertEquals(customerId, retrievedCustomerId);
    }

    @Test
    @DisplayName("Should return 0 for invalid session when getting customer ID")
    void shouldReturnZeroForInvalidSessionWhenGettingCustomerId() {
        // When
        int customerId = sessionService.getCustomerId("invalid-session-key");

        // Then
        assertEquals(0, customerId);
    }

    @Test
    @DisplayName("Should return 0 for null session when getting customer ID")
    void shouldReturnZeroForNullSessionWhenGettingCustomerId() {
        // When
        int customerId = sessionService.getCustomerId(null);

        // Then
        assertEquals(0, customerId);
    }

    @Test
    @DisplayName("Should handle multiple customers independently")
    void shouldHandleMultipleCustomersIndependently() {
        // Given
        int customer1 = 100;
        int customer2 = 200;

        // When
        Session session1 = sessionService.createOrGetSession(customer1);
        Session session2 = sessionService.createOrGetSession(customer2);

        // Then
        assertNotEquals(session1.getSessionKey(), session2.getSessionKey());
        assertEquals(customer1, session1.getCustomerId());
        assertEquals(customer2, session2.getCustomerId());
        
        assertTrue(sessionService.validateSession(session1.getSessionKey()));
        assertTrue(sessionService.validateSession(session2.getSessionKey()));
        
        assertEquals(customer1, sessionService.getCustomerId(session1.getSessionKey()));
        assertEquals(customer2, sessionService.getCustomerId(session2.getSessionKey()));
    }
}

