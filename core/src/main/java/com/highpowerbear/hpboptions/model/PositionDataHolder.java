package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.*;
import com.ib.client.Types;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractOptionDataHolder {

    private final int ibPnlRequestId;

    public PositionDataHolder(Instrument instrument, int ibMktDataRequestId, int ibPnlRequestId, Types.Right right, double strike, LocalDate expirationDate, int positionSize) {
        super(DataHolderType.POSITION, instrument, ibMktDataRequestId, right, strike, expirationDate);
        this.ibPnlRequestId = ibPnlRequestId;

        PositionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                PositionDataField.POSITION_SIZE,
                PositionDataField.UNREALIZED_PNL
        ).collect(Collectors.toSet()));

        updatePositionSize(positionSize);
    }
    public void updatePositionSize(int positionSize) {
        update(PositionDataField.POSITION_SIZE, positionSize);
    }

    public void updateUnrealizedPnl(double unrealizedPl) {
        update(PositionDataField.UNREALIZED_PNL, unrealizedPl);
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public String getUnderlyingSymbol() {
        return getInstrument().getUnderlyingSymbol();
    }

    public int getPositionSize() {
        return getCurrent(PositionDataField.POSITION_SIZE).intValue();
    }

    public double getUnrealizedPnl() {
        return getCurrent(PositionDataField.UNREALIZED_PNL).doubleValue();
    }
}
