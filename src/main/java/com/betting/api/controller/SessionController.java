package com.betting.api.controller;

import com.betting.api.exception.BettingException;
import com.betting.core.model.Session;
import com.betting.core.service.SessionService;
import com.betting.infrastructure.http.router.PathParam;
import com.betting.infrastructure.http.router.Route;
import com.betting.util.BettingValidator;

public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Route(method = "GET", path = "/{customerId}/session")
    public String getSession(@PathParam("customerId") int customerId) {
        BettingValidator.customer(customerId);
        
        try {
            Session session = sessionService.createOrGetSession(customerId);
            return session.getSessionKey();
        } catch (Exception e) {
            throw new BettingException(BettingException.ErrorCode.INTERNAL_ERROR, 
                "Failed to create session for customer: " + customerId);
        }
    }
}
