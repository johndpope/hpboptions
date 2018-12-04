package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.BasicField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedField;

import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class Underlying extends AbstractDataHolder {

    public Underlying(Instrument instrument, int ibRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibRequestId);

        Stream.of(
                BasicField.BID,
                BasicField.ASK,
                BasicField.LAST,
                BasicField.CLOSE,
                BasicField.BID_SIZE,
                BasicField.ASK_SIZE,
                BasicField.LAST_SIZE,
                BasicField.VOLUME,
                BasicField.OPTION_IMPLIED_VOL,
                DerivedField.CHANGE_PCT,
                DerivedField.OPTION_VOLUME,
                DerivedField.OPTION_OPEN_INTEREST
        ).forEach(fieldsToDisplay::add);

        determineGenericTicks();
    }

    public double getOptionImpliedVol() {
        return valueMap.get(BasicField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public int getOptionVolume() {
        return valueMap.get(DerivedField.OPTION_VOLUME).intValue();
    }

    public int getOptionOpenInterest() {
        return valueMap.get(DerivedField.OPTION_OPEN_INTEREST).intValue();
    }

}
