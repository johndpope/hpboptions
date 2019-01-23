package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
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

    public boolean isBidFalling() {
        double currentBid = getCurrent(BasicMktDataField.BID).doubleValue();
        double oldBid = getOld(BasicMktDataField.BID).doubleValue();

        return (HopUtil.isValidPrice(currentBid) && isValidPrice(oldBid) && currentBid < oldBid);
    }

    public boolean isAskRising() {
        double currentAsk = getCurrent(BasicMktDataField.ASK).doubleValue();
        double oldAsk = getOld(BasicMktDataField.ASK).doubleValue();

        return (HopUtil.isValidPrice(currentAsk) && isValidPrice(oldAsk) && currentAsk > oldAsk);
    }
}
