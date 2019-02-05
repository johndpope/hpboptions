package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 1/9/2019.
 */
public class OrderDataHolder extends AbstractDataHolder {

    private final HopOrder hopOrder;

    public OrderDataHolder(Instrument instrument, int ibMktDataRequestId, HopOrder hopOrder) {
        super(DataHolderType.ORDER, instrument, ibMktDataRequestId);
        this.hopOrder = hopOrder;

        id = id + "-" + hopOrder.getOrderId();
    }

    public HopOrder getHopOrder() {
        return hopOrder;
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
