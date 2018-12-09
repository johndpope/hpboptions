package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.OptionDataField;

/**
 * Created by robertk on 12/5/2018.
 */
public class ChainDataHolder extends AbstractOptionDataHolder {

    public ChainDataHolder(Instrument instrument, int ibMktDataRequestId) {
        super(DataHolderType.CHAIN, instrument, ibMktDataRequestId);

        OptionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
    }
}
