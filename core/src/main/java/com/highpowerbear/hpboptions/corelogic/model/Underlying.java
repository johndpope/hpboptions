package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.enums.FieldType;

/**
 * Created by robertk on 11/26/2018.
 */
public class Underlying extends AbstractDataHolder {

    public Underlying(Instrument instrument, int ibRequestId) {
        super(instrument, ibRequestId);
    }

    @Override
    public String createMessage(FieldType fieldType) {
        return "underlying," + ibRequestId + "," + fieldType + "," + valueMap.get(fieldType);
    }

    @Override
    public String getWsTopic() {
        return CoreSettings.WS_TOPIC_UNDERLYING;
    }
}
