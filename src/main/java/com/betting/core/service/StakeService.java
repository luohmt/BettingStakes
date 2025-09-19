package com.betting.core.service;

import java.util.List;

public interface StakeService {
    void submitStake(int customerId, int betOfferId, int stake);
    List<String> getTop20Stakes(int betOfferId);
}
