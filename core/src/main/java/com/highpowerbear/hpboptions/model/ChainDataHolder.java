package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.ib.client.Types;

import java.time.LocalDate;

/**
 * Created by robertk on 12/5/2018.
 */
public class ChainDataHolder extends AbstractOptionDataHolder {

    public ChainDataHolder(Instrument instrument, int ibMktDataRequestId, Types.Right right, double strike, LocalDate expirationDate) {
        super(DataHolderType.CHAIN, instrument, ibMktDataRequestId, right, strike, expirationDate);

        OptionDataField.getFields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
    }
}
