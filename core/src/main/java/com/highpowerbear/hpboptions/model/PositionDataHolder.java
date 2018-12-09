package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.OptionDataField;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractOptionDataHolder {

    private int position;
    private double unrealizedPl;

    public PositionDataHolder(Instrument instrument, int ibMktDataRequestId) {
        super(DataHolderType.POSITION, instrument, ibMktDataRequestId);

        OptionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
    }

    public void updatePosition(int position) {
        this.position = position;
    }

    public void updateUnrelaizedPl(double unrealizedPl) {
        this.unrealizedPl = unrealizedPl;
    }

    public String createPositionMessage() {
        return id + ",position," + position;
    }

    public String createUnrealizedPlMessage() {
        return id + ",unrealizedPl," + unrealizedPl;
    }

    public int getPosition() {
        return position;
    }

    public double getUnrealizedPl() {
        return unrealizedPl;
    }
}
