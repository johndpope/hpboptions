package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;

import java.util.Map;

/**
 * Created by robertk on 11/26/2018.
 */
public class ChainItem extends AbstractDataHolder {

    public ChainItem(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    @Override
    protected void initMapSpecific(Map<FieldType, Number> map) {
    }

    @Override
    public String getMessagePrefix() {
        return "chain";
    }
}
