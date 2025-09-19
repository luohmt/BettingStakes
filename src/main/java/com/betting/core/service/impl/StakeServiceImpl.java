package com.betting.core.service.impl;

import com.betting.api.exception.BettingException;
import com.betting.core.service.StakeService;
import com.betting.util.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Optimized Stake Service Implementation using Skip List
 * <p>
 * Features:
 * - O(log n) insertion and O(1) top retrieval
 * - Thread-safe operations
 * - Memory efficient design
 * - Proper handling of duplicate stake values
 */
public class StakeServiceImpl implements StakeService {


    // Configuration constants
    private static final int MAX_STAKE_LIMIT = 1_000_000;
    private static final int TOP_STAKES_LIMIT = 20;

    /**
     * Customer stakes mapping: betOfferId -> (customerId -> maxStake)
     */
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> customerStakes = new ConcurrentHashMap<>();

    /**
     * Skip list for sorted stakes: betOfferId -> (stake -> Set<customerId>, descending order)
     * Using Set to handle multiple customers with same stake value
     */
    private final ConcurrentHashMap<Integer, ConcurrentSkipListMap<Integer, Set<Integer>>> skipListStakes = new ConcurrentHashMap<>();

    @Override
    public void submitStake(int customerId, int betOfferId, int stake) {
        validateInput(customerId, betOfferId, stake);

        Logger.info("Submitting stake: customerId=%s, betOfferId=%s, stake=%s",
                customerId, betOfferId, stake);

        // Get or create customer stakes map for this betting offer
        ConcurrentHashMap<Integer, Integer> customerMap = customerStakes.computeIfAbsent(betOfferId,
                k -> new ConcurrentHashMap<>());

        // Get the current maximum stake for this customer
        Integer oldStake = customerMap.get(customerId);

        // Only update if this is a new stake or higher than existing
        if (oldStake == null || stake > oldStake) {
            // Update the customer's maximum stake
            customerMap.put(customerId, stake);

            // Get or create skip list for this betting offer
            ConcurrentSkipListMap<Integer, Set<Integer>> skipList = skipListStakes.computeIfAbsent(betOfferId,
                    k -> new ConcurrentSkipListMap<>(Collections.reverseOrder()));

            // Remove customer from old stake position if exists
            if (oldStake != null) {
                removeCustomerFromStake(skipList, oldStake, customerId);
            }

            // Add customer to new stake position
            addCustomerToStake(skipList, stake, customerId);

            Logger.info("Updated stake: customerId=%s, betOfferId=%s, oldStake=%s, newStake=%s",
                    customerId, betOfferId, oldStake, stake);
        }
    }

    /**
     * Validates input parameters
     */
    private void validateInput(int customerId, int betOfferId, int stake) {
        if (customerId <= 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_CUSTOMER_ID,
                    "Customer ID must be positive, got: " + customerId);
        }

        if (betOfferId <= 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_BET_OFFER_ID,
                    "Bet offer ID must be positive, got: " + betOfferId);
        }

        if (stake < 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_STAKE_AMOUNT,
                    "Stake amount cannot be negative, got: " + stake);
        }

        if (stake > MAX_STAKE_LIMIT) {
            throw new BettingException(BettingException.ErrorCode.STAKE_TOO_HIGH,
                    "Stake amount exceeds maximum limit of " + MAX_STAKE_LIMIT + ", got: " + stake);
        }
    }

    /**
     * Removes a customer from a specific stake position
     */
    private void removeCustomerFromStake(ConcurrentSkipListMap<Integer, Set<Integer>> skipList,
                                         int stake, int customerId) {
        Set<Integer> customers = skipList.get(stake);
        if (customers != null) {
            customers.remove(customerId);
            if (customers.isEmpty()) {
                skipList.remove(stake);
            }
        }
    }

    /**
     * Adds a customer to a specific stake position
     */
    private void addCustomerToStake(ConcurrentSkipListMap<Integer, Set<Integer>> skipList,
                                    int stake, int customerId) {
        skipList.computeIfAbsent(stake, k -> ConcurrentHashMap.newKeySet()).add(customerId);
    }

    @Override
    public List<String> getTop20Stakes(int betOfferId) {
        if (betOfferId <= 0) {
            throw new BettingException(BettingException.ErrorCode.INVALID_BET_OFFER_ID,
                    "Bet offer ID must be positive, got: " + betOfferId);
        }

        Logger.info("Getting top %s stakes for betOfferId=%s", TOP_STAKES_LIMIT, betOfferId);

        // Get the skip list for this betting offer
        ConcurrentSkipListMap<Integer, Set<Integer>> skipList = skipListStakes.get(betOfferId);
        if (skipList == null || skipList.isEmpty()) {
            Logger.info("No stakes found for betOfferId=%s", betOfferId);
            return Collections.emptyList();
        }

        // Skip list is already sorted in descending order (highest stakes first)
        // Collect top stakes with proper format
        List<String> result = skipList.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(customerId -> customerId + "=" + entry.getKey()))
                .limit(TOP_STAKES_LIMIT)
                .toList();

        Logger.info("Retrieved %s stakes for betOfferId=%s", result.size(), betOfferId);
        return result;
    }
}
