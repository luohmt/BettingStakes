package com.betting.api.controller;

import com.betting.api.exception.BettingException;
import com.betting.core.service.SessionService;
import com.betting.core.service.StakeService;
import com.betting.infrastructure.http.router.BodyParam;
import com.betting.infrastructure.http.router.PathParam;
import com.betting.infrastructure.http.router.QueryParam;
import com.betting.infrastructure.http.router.Route;
import com.betting.util.Logger;
import com.betting.util.BettingValidator;

import java.util.List;

public class StakeController {
    private final SessionService sessionService;
    private final StakeService stakeService;

    public StakeController(SessionService sessionService, StakeService stakeService) {
        this.sessionService = sessionService;
        this.stakeService = stakeService;
    }

    @Route(method = "POST", path = "/{betOfferId}/stake")
    public String postStake(@PathParam("betOfferId") int betOfferId,
                            @QueryParam("sessionkey") String sessionKey,
                            @BodyParam("stake") int stake) {
        // Business validation - reads like business requirements
        BettingValidator.betOffer(betOfferId);
        BettingValidator.stakeAmount(stake);
        int customerId = BettingValidator.authenticateCustomer(sessionKey, sessionService);

        try {
            stakeService.submitStake(customerId, betOfferId, stake);
            return ""; // return empty string on success
        } catch (BettingException e) {
            // Re-throw BettingException as-is
            throw e;
        } catch (Exception e) {
            Logger.error("Error submitting stake", e, e.getMessage());
            throw new BettingException(BettingException.ErrorCode.INTERNAL_ERROR, 
                "Failed to submit stake for customer: " + customerId);
        }
    }


    @Route(method = "GET", path = "/{betOfferId}/highstakes")
    public String getHighStakes(@PathParam("betOfferId") int betOfferId) {
        BettingValidator.betOffer(betOfferId);

        try {
            List<String> top20 = stakeService.getTop20Stakes(betOfferId);
            return String.join(",", top20);
        } catch (BettingException e) {
            // Re-throw BettingException as-is
            throw e;
        } catch (Exception e) {
            throw new BettingException(BettingException.ErrorCode.INTERNAL_ERROR,
                    "Failed to retrieve top stakes for betOfferId: " + betOfferId);
        }
    }
}


