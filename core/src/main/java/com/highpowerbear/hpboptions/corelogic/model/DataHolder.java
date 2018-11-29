package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;

/**
 * Created by robertk on 11/26/2018.
 */
public interface DataHolder {

    Instrument getInstrument();
    int getIbRequestId();
    void updateValue(FieldType fieldType, Number value);
    String createMessage(FieldType fieldType);

    double getBid();
    double getAsk();
    double getLast();
    double getClose();
    double getChangePct();
    int getBidSize();
    int getAskSize();
    int getLastSize();
    int getVolume();
}
