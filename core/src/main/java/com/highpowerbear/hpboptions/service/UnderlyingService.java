package com.highpowerbear.hpboptions.service;

import com.ib.client.Bar;

/**
 * Created by robertk on 5/28/2019.
 */
public interface UnderlyingService extends MarketDataService {
    void historicalDataReceived(int requestId, Bar bar);
    void historicalDataEndReceived(int requestId);
}
