package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.model.Instrument;

/**
 * Created by robertk on 5/28/2019.
 */
public class LinearDataHolder extends AbstractMarketDataHolder {

    public LinearDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId) {
        super(type, instrument, ibMktDataRequestId);
    }
}
