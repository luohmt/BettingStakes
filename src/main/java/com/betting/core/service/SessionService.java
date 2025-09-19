package com.betting.core.service;

import com.betting.core.model.Session;

public interface SessionService {
    Session createOrGetSession(int customerId);

    boolean validateSession(String sessionKey);

    int getCustomerId(String sessionKey);
}
