package com.betting.core.service;

import com.betting.api.exception.BettingException;
import com.betting.core.service.impl.StakeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StakeService interface
 * Tests core stake management functionality
 */
@DisplayName("StakeService Tests")
class StakeServiceTest {

    private StakeService stakeService;

    @BeforeEach
    void setUp() {
        stakeService = new StakeServiceImpl();
    }

    @Test
    @DisplayName("Should submit valid stake")
    void shouldSubmitValidStake() {
        // Given
        int customerId = 123;
        int betOfferId = 1;
        int stake = 100;

        // When & Then
        assertDoesNotThrow(() -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid customer ID")
    void shouldThrowExceptionForInvalidCustomerId() {
        // Given
        int customerId = 0;
        int betOfferId = 1;
        int stake = 100;

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should throw exception for negative customer ID")
    void shouldThrowExceptionForNegativeCustomerId() {
        // Given
        int customerId = -1;
        int betOfferId = 1;
        int stake = 100;

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid bet offer ID")
    void shouldThrowExceptionForInvalidBetOfferId() {
        // Given
        int customerId = 123;
        int betOfferId = 0;
        int stake = 100;

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should throw exception for negative stake")
    void shouldThrowExceptionForNegativeStake() {
        // Given
        int customerId = 123;
        int betOfferId = 1;
        int stake = -50;

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should throw exception for stake too high")
    void shouldThrowExceptionForStakeTooHigh() {
        // Given
        int customerId = 123;
        int betOfferId = 1;
        int stake = 1000001; // Exceeds maximum limit

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.submitStake(customerId, betOfferId, stake);
        });
    }

    @Test
    @DisplayName("Should keep maximum stake per customer")
    void shouldKeepMaximumStakePerCustomer() {
        // Given
        int customerId = 456;
        int betOfferId = 1;
        int firstStake = 100;
        int secondStake = 200;

        // When
        stakeService.submitStake(customerId, betOfferId, firstStake);
        stakeService.submitStake(customerId, betOfferId, secondStake);

        // Then
        List<String> topStakes = stakeService.getTop20Stakes(betOfferId);
        assertTrue(topStakes.contains("456=200"));
        assertFalse(topStakes.contains("456=100"));
    }

    @Test
    @DisplayName("Should return top stakes for valid bet offer")
    void shouldReturnTopStakesForValidBetOffer() {
        // Given
        int customerId1 = 1;
        int customerId2 = 2;
        int betOfferId = 1;
        int stake1 = 100;
        int stake2 = 200;

        // When
        stakeService.submitStake(customerId1, betOfferId, stake1);
        stakeService.submitStake(customerId2, betOfferId, stake2);

        // Then
        List<String> topStakes = stakeService.getTop20Stakes(betOfferId);
        assertEquals(2, topStakes.size());
        assertTrue(topStakes.contains("1=100"));
        assertTrue(topStakes.contains("2=200"));
    }

    @Test
    @DisplayName("Should return empty list for non-existent bet offer")
    void shouldReturnEmptyListForNonExistentBetOffer() {
        // Given
        int betOfferId = 999;

        // When
        List<String> topStakes = stakeService.getTop20Stakes(betOfferId);

        // Then
        assertTrue(topStakes.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for invalid bet offer ID when getting top stakes")
    void shouldThrowExceptionForInvalidBetOfferIdWhenGettingTopStakes() {
        // Given
        int betOfferId = 0;

        // When & Then
        assertThrows(BettingException.class, () -> {
            stakeService.getTop20Stakes(betOfferId);
        });
    }

    @Test
    @DisplayName("Should handle multiple customers and bet offers")
    void shouldHandleMultipleCustomersAndBetOffers() {
        // Given
        int customer1 = 1;
        int customer2 = 2;
        int betOffer1 = 1;
        int betOffer2 = 2;
        int stake1 = 100;
        int stake2 = 200;

        // When
        stakeService.submitStake(customer1, betOffer1, stake1);
        stakeService.submitStake(customer2, betOffer1, stake2);
        stakeService.submitStake(customer1, betOffer2, stake1);

        // Then
        List<String> topStakes1 = stakeService.getTop20Stakes(betOffer1);
        List<String> topStakes2 = stakeService.getTop20Stakes(betOffer2);

        assertEquals(2, topStakes1.size());
        assertEquals(1, topStakes2.size());
        assertTrue(topStakes1.contains("1=100"));
        assertTrue(topStakes1.contains("2=200"));
        assertTrue(topStakes2.contains("1=100"));
    }

    @Test
    @DisplayName("Should maintain top 20 stakes limit")
    void shouldMaintainTop20StakesLimit() {
        // Given
        int betOfferId = 1;
        
        // When - Submit 25 stakes
        for (int i = 1; i <= 25; i++) {
            stakeService.submitStake(i, betOfferId, i * 10);
        }

        // Then
        List<String> topStakes = stakeService.getTop20Stakes(betOfferId);
        assertEquals(20, topStakes.size());
        
        // Should contain highest stakes (6-25)
        assertTrue(topStakes.contains("25=250"));
        assertTrue(topStakes.contains("6=60"));
        // Should not contain lowest stakes (1-5)
        assertFalse(topStakes.contains("1=10"));
        assertFalse(topStakes.contains("5=50"));
    }
}

