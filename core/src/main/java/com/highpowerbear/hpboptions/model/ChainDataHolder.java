package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.OptionDataField;

/**
 * Created by robertk on 12/5/2018.
 */
public class ChainDataHolder extends AbstractOptionDataHolder {

    public ChainDataHolder(OptionInstrument instrument, int ibMktDataRequestId) {
        super(DataHolderType.CHAIN, instrument, ibMktDataRequestId);

        OptionDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
    }

    @Override
    public String createMessage(DataField dataField) {
        return getType().name().toLowerCase() + "-" + getInstrument().getStrike() + "," +
                CoreUtil.toCamelCase(getInstrument().getRight().name() + "_" + dataField.name()) + "," +
                getOld(dataField) + "," +
                getCurrent(dataField);
    }
}
