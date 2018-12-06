package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractOptionDataHolder {

    private int position;

    public PositionDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.POSITION, instrument, ibRequestId);
    }

    public void updatePosition(int position) {
        this.position = position;
    }

    public String createPositionMessage() {
        return id + ",position," + position;
    }

    public int getPosition() {
        return position;
    }
}
