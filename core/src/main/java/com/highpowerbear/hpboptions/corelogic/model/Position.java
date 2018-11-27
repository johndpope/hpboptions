package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;

/**
 * Created by robertk on 11/26/2018.
 */
public class Position extends AbstractDataHolder {

    public Position(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    @Override
    public String createMessage(FieldType fieldType) {
        return "position," + ibRequestId + "," + fieldType + "," + valueMap.get(fieldType);
    }
}
