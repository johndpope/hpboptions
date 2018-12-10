package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractOptionDataHolder {

    public PositionDataHolder(Instrument instrument, int ibMktDataRequestId) {
        super(DataHolderType.POSITION, instrument, ibMktDataRequestId);
        PositionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                PositionDataField.POSITION_SIZE,
                PositionDataField.UNREALIZED_PL
        ).collect(Collectors.toSet()));
    }

    public void updatePosition(int position) {
        update(PositionDataField.POSITION_SIZE, position);
    }

    public void updateUnrelaizedPl(double unrealizedPl) {
        update(PositionDataField.UNREALIZED_PL, unrealizedPl);
    }

    public int getPosition() {
        return getCurrent(PositionDataField.POSITION_SIZE).intValue();
    }

    public double getUnrealizedPl() {
        return getCurrent(PositionDataField.UNREALIZED_PL).doubleValue();
    }
}
