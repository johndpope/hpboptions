package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.FieldType;

/**
 * Created by robertk on 11/26/2018.
 */
public class Underlying extends AbstractDataHolder {

    public Underlying(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    @Override
    public String createMessage(FieldType fieldType) {
        String value = fieldType == FieldType.VOLUME ?
                String.valueOf(getVolume()) :
                valueMap.get(fieldType).toString();

        return "underlying," + ibRequestId + "," + fieldType.toCamelCase() + "," + value;
    }

    @Override
    public int getVolume() {
        int volume = valueMap.get(FieldType.VOLUME).intValue();
        if (CoreUtil.isValidSize(volume)) {
            volume *= 100;
        }
        return volume;
    }
}