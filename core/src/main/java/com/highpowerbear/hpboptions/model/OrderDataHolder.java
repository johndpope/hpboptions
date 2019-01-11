package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.ib.client.TickType;

/**
 * Created by robertk on 1/9/2019.
 */
public class OrderDataHolder extends AbstractDataHolder implements OptionDataHolder {

    private final HopOrder hopOrder;

    public OrderDataHolder(OptionInstrument instrument, int ibMktDataRequestId, HopOrder hopOrder) {
        super(DataHolderType.ORDER, instrument, ibMktDataRequestId);
        this.hopOrder = hopOrder;

        id = id + "-" + hopOrder.getOrderId();
    }

    public HopOrder getHopOrder() {
        return hopOrder;
    }

    @Override
    public OptionInstrument getInstrument() {
        return (OptionInstrument) instrument;
    }

    @Override
    public void updateOptionData(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
    }

    @Override
    public void recalculateOptionData() {
    }
}
