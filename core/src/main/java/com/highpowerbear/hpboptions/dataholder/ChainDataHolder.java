package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.OptionDataField;
import com.highpowerbear.hpboptions.model.OptionInstrument;

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
        return getType().name().toLowerCase() + "-" + String.valueOf(getInstrument().getStrike()).replace(".", "-") + "," +
                HopUtil.toCamelCase(getInstrument().getRight().name() + "_" + dataField.name()) + "," +
                getOld(dataField) + "," +
                getCurrent(dataField);
    }
}
