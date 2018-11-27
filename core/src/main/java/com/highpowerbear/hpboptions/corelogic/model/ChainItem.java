package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.enums.FieldType;

/**
 * Created by robertk on 11/26/2018.
 */
public class ChainItem extends AbstractDataHolder {

    public ChainItem(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    @Override
    public String createMessage(FieldType fieldType) {
        return "chainItem," + ibRequestId + "," + fieldType + "," + valueMap.get(fieldType);
    }

    @Override
    public String getWsTopic() {
        return CoreSettings.WS_TOPIC_CHAIN;
    }
}
