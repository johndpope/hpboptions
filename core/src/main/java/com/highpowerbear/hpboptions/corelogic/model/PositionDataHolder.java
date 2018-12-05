package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 12/5/2018.
 */
public class PositionDataHolder extends AbstractDataHolder {

    public PositionDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.POSITION, instrument, ibRequestId);
    }
}
