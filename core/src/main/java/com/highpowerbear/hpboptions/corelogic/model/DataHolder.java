package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.*;

/**
 * Created by robertk on 12/3/2018.
 */
public interface DataHolder {
    void updateField(BasicField field, Number value);
    void calculateField(DerivedField field);
    DataHolderType getType();
    Instrument getInstrument();
    int getIbRequestId();
    String createMessage(Field field);
    boolean isDisplayed(Field field);
    String getGenericTicks();
}
