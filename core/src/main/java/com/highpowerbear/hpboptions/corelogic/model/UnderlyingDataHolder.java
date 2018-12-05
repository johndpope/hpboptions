package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.BasicField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedField;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    public UnderlyingDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibRequestId);

        addFieldsToDisplay(Stream.of(
                BasicField.OPTION_IMPLIED_VOL,
                DerivedField.OPTION_VOLUME,
                DerivedField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));
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
