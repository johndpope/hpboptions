package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;

import java.util.Map;

/**
 * Created by robertk on 11/26/2018.
 */
public class Underlying extends AbstractDataHolder {

    public Underlying(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    protected void initMapSpecific(Map<FieldType, Number> map) {
        map.put(FieldType.OPTION_VOLUME, -1);
        map.put(FieldType.OPTION_AVG_VOLUME, -1);
        map.put(FieldType.OPTION_OPEN_INTEREST, -1);
    }

    @Override
    public String getMessagePrefix() {
        return "underlying";
    }
}