package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;

/**
 * Created by robertk on 12/5/2018.
 */
public class ChainDataHolder extends AbstractOptionDataHolder {

    public ChainDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.CHAIN, instrument, ibRequestId);
    }
}
