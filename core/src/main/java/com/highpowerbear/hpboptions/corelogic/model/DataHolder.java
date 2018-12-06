package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.*;

/**
 * Created by robertk on 12/3/2018.
 */
public interface DataHolder {
    String getId();
    void updateField(BasicMktDataField field, Number value);
    void calculateField(DerivedMktDataField field);
    DataHolderType getType();
    Instrument getInstrument();
    int getIbRequestId();
    String createMessage(MktDataField mktDataField);
    boolean isDisplayed(MktDataField mktDataField);
    String getGenericTicks();
}
