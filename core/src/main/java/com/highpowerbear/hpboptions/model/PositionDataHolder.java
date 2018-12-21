package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.PositionDataField;
import com.ib.client.Types;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractOptionDataHolder {

    private final int ibPnlRequestId;

    public PositionDataHolder(OptionInstrument instrument, int ibMktDataRequestId, int ibPnlRequestId) {
        super(DataHolderType.POSITION, instrument, ibMktDataRequestId);
        this.ibPnlRequestId = ibPnlRequestId;

        PositionDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                PositionDataField.POSITION_SIZE,
                PositionDataField.UNREALIZED_PNL
        ).collect(Collectors.toSet()));
    }

    @JsonIgnore
    public Types.Right getRight() { //sorting
        return getInstrument().getRight();
    }

    @JsonIgnore
    public double getStrike() { //sorting
        return getInstrument().getStrike();
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public void updatePositionSize(int positionSize) {
        update(PositionDataField.POSITION_SIZE, positionSize);
    }

    public void updateUnrealizedPnl(double unrealizedPl) {
        update(PositionDataField.UNREALIZED_PNL, unrealizedPl);
    }

    public int getPositionSize() {
        return getCurrent(PositionDataField.POSITION_SIZE).intValue();
    }

    public double getUnrealizedPnl() {
        return getCurrent(PositionDataField.UNREALIZED_PNL).doubleValue();
    }
}
