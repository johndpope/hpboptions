package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 1/9/2019.
 */
public class OrderDataHolder extends AbstractDataHolder {

    private final HopOrder hopOrder;

    public OrderDataHolder(OptionInstrument instrument, int ibMktDataRequestId, HopOrder hopOrder) {
        super(DataHolderType.ORDER, instrument, ibMktDataRequestId);
        this.hopOrder = hopOrder;
    }

    public HopOrder getHopOrder() {
        return hopOrder;
    }

    @Override
    public OptionInstrument getInstrument() {
        return (OptionInstrument) instrument;
    }
}
