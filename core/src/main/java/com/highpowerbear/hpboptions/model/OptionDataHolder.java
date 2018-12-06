package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 12/6/2018.
 */
public interface OptionDataHolder {
    void updateOptionData(double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    String createOptionDataMessage();
    void updateDaysToExpiration(int daysToExpiration);
    String createDaysToExpirationMessage();
}
