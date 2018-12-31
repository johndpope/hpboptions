package com.highpowerbear.hpboptions.logic;

import com.ib.client.ContractDetails;
import com.ib.client.TickType;

/**
 * Created by robertk on 12/31/2018.
 */
public interface DataService {
    void mktDataReceived(int requestId, int tickType, Number value);
    void optionDataReceived(int requestId, TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    void contractDetailsReceived(ContractDetails contractDetails);
    void contractDetailsEndReceived(int requestId);
}
