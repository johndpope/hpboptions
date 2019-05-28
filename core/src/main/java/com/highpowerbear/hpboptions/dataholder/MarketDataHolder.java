package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.field.BasicMarketDataField;
import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.DerivedMarketDataField;
import com.highpowerbear.hpboptions.model.Instrument;

/**
 * Created by robertk on 12/3/2018.
 */
public interface MarketDataHolder {
    String getId();
    DataHolderType getType();
    Instrument getInstrument();
    int getDisplayRank();
    void updateField(BasicMarketDataField field, Number value);
    void calculateField(DerivedMarketDataField field);
    int getIbMktDataRequestId();
    String createMessage(DataField dataField);
    boolean isSendMessage(DataField dataField);
    String getGenericTicks();
}
