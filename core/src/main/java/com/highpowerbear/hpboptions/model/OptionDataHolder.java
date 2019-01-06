package com.highpowerbear.hpboptions.model;

import com.ib.client.TickType;

/**
 * Created by robertk on 12/6/2018.
 */
public interface OptionDataHolder extends DataHolder {
    OptionInstrument getInstrument();
    void updateOptionData(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    void recalculateOptionData();
    boolean riskDataSourceFieldsReady();
}
