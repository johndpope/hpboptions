package com.highpowerbear.hpboptions.service;

import com.ib.client.Bar;
import com.ib.client.ContractDetails;
import com.ib.client.TickType;

/**
 * Created by robertk on 12/31/2018.
 */
public interface MarketDataService {
    void mktDataReceived(int requestId, int tickType, Number value);
    void optionDataReceived(int requestId, TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    void contractDetailsReceived(int requestId, ContractDetails contractDetails);
    void contractDetailsEndReceived(int requestId);
    void historicalDataReceived(int requestId, Bar bar);
    void historicalDataEndReceived(int requestId);
}
