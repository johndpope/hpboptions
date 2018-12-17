package com.highpowerbear.hpboptions.model;

import com.ib.client.TickType;

/**
 * Created by robertk on 12/6/2018.
 */
public interface OptionDataHolder {
    void updateOptionData(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    boolean isOptionDataValid();
}
