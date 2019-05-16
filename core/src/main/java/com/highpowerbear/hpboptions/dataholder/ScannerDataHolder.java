package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.model.Instrument;

/**
 * Created by robertk on 5/16/2019.
 */
public class ScannerDataHolder extends AbstractUnderlyingDataHolder {

    public ScannerDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.SCANNER, instrument, ibMktDataRequestId, ibHistDataRequestId);
    }
}
