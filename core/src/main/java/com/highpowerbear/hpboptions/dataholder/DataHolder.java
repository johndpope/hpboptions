package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.DataField;

/**
 * Created by robertk on 6/6/2019.
 */
public interface DataHolder {
    String getId();
    DataHolderType getType();
    String createMessage(DataField dataField);
    boolean isSendMessage(DataField dataField);
}
