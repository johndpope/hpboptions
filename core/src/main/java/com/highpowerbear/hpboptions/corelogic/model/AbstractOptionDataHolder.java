package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 12/6/2018.
 */
public class AbstractOptionDataHolder extends AbstractDataHolder implements OptionDataHolder {

    private OptionData optionData;
    private int daysToExpiration = -1;

    public AbstractOptionDataHolder(DataHolderType type, Instrument instrument, int ibRequestId) {
        super(type, instrument, ibRequestId);
        optionData = new OptionData();
    }

    @Override
    public void updateOptionData(double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        // TODO calculate timeValue and timeValuePct
        double timeValue = 0;
        double timeValuePct = 0;

        optionData.update(delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice, timeValue, timeValuePct);
    }

    @Override
    public String createOptionDataMessage() {
        return id + ",optionData," + optionData.getDelta() + "," + optionData.getGamma() + "," + optionData.getImpliedVolatility() + "," + optionData.getUnderlyingPrice();
    }

    @Override
    public void updateDaysToExpiration(int daysToExpiration) {
        this.daysToExpiration = daysToExpiration;
    }

    @Override
    public String createDaysToExpirationMessage() {
        return id + ",daysToExpiration," + daysToExpiration;
    }

    public OptionData getOptionData() {
        return optionData;
    }

    public int getDaysToExpiration() {
        return daysToExpiration;
    }
}
