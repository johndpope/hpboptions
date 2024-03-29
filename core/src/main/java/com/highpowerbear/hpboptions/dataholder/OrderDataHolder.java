package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.field.BasicMarketDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.model.HopOrder;
import com.highpowerbear.hpboptions.model.Instrument;

/**
 * Created by robertk on 1/9/2019.
 */
public class OrderDataHolder extends AbstractMarketDataHolder {

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
        double currentBid = getCurrent(BasicMarketDataField.BID).doubleValue();
        double oldBid = getOld(BasicMarketDataField.BID).doubleValue();

        return (HopUtil.isValidPrice(currentBid) && isValidPrice(oldBid) && currentBid < oldBid);
    }

    public boolean isAskRising() {
        double currentAsk = getCurrent(BasicMarketDataField.ASK).doubleValue();
        double oldAsk = getOld(BasicMarketDataField.ASK).doubleValue();

        return (HopUtil.isValidPrice(currentAsk) && isValidPrice(oldAsk) && currentAsk > oldAsk);
    }
}
