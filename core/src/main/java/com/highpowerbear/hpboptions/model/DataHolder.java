package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;

/**
 * Created by robertk on 12/3/2018.
 */
public interface DataHolder {
    String getId();
    DataHolderType getType();
    Instrument getInstrument();
    int getDisplayRank();
    void updateField(BasicMktDataField field, Number value);
    void calculateField(DerivedMktDataField field);
    int getIbMktDataRequestId();
    String createMessage(DataField dataField);
    boolean isSendMessage(DataField dataField);
    String getGenericTicks();
}
