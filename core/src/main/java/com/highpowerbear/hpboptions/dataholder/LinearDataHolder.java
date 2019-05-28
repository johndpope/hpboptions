package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.LinearDataField;
import com.highpowerbear.hpboptions.model.Instrument;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 5/28/2019.
 */
public class LinearDataHolder extends AbstractMarketDataHolder {

    private final int ibPnlRequestId;

    public LinearDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId, int ibPnlRequestId) {
        super(type, instrument, ibMktDataRequestId);
        this.ibPnlRequestId = ibPnlRequestId;

        LinearDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                LinearDataField.POSITION_SIZE,
                LinearDataField.UNREALIZED_PNL
        ).collect(Collectors.toSet()));
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public void updatePositionSize(int positionSize) {
        update(LinearDataField.POSITION_SIZE, positionSize);
    }

    public void updateUnrealizedPnl(double unrealizedPl) {
        update(LinearDataField.UNREALIZED_PNL, unrealizedPl);
    }

    public int getPositionSize() {
        return getCurrent(LinearDataField.POSITION_SIZE).intValue();
    }

    public double getUnrealizedPnl() {
        return getCurrent(LinearDataField.UNREALIZED_PNL).doubleValue();
    }
}
