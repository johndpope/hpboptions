package com.highpowerbear.hpboptions.model;

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
    int getIbMktDataRequestId();
    String createMessage(DataField dataField);
    boolean isSendMessage(DataField dataField);
    String getGenericTicks();
}
