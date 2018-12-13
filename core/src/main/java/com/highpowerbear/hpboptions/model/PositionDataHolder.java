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

    public PositionDataHolder(Instrument instrument, int ibMktDataRequestId, Types.Right right, double strike, LocalDate expirationDate, int positionSize) {
        super(DataHolderType.POSITION, instrument, ibMktDataRequestId, right, strike, expirationDate);

        PositionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                PositionDataField.POSITION_SIZE,
                PositionDataField.UNREALIZED_PL
        ).collect(Collectors.toSet()));

        updatePositionSize(positionSize);
    }

    public String getUnderlyingSymbol() {
        return getInstrument().getUnderlyingSymbol();
    }

    public void updatePositionSize(int positionSize) {
        update(PositionDataField.POSITION_SIZE, positionSize);
    }

    public void updateUnrelaizedPl(double unrealizedPl) {
        update(PositionDataField.UNREALIZED_PL, unrealizedPl);
    }

    public int getPositionSize() {
        return getCurrent(PositionDataField.POSITION_SIZE).intValue();
    }

    public double getUnrealizedPl() {
        return getCurrent(PositionDataField.UNREALIZED_PL).doubleValue();
    }
}
