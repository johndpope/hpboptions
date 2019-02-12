package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.model.OptionInstrument;
import com.ib.client.TickType;

/**
 * Created by robertk on 12/6/2018.
 */
public interface OptionDataHolder extends MarketDataHolder {
    OptionInstrument getInstrument();
    void optionDataReceived(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice);
    void recalculateOptionData();
}
