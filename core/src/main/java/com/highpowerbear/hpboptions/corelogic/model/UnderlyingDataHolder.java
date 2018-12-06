package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    private double timeValue;
    private double delta;
    private double gamma;

    public UnderlyingDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibRequestId);

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));
    }

    public double getOptionImpliedVol() {
        return valueMap.get(BasicMktDataField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public int getOptionVolume() {
        return valueMap.get(DerivedMktDataField.OPTION_VOLUME).intValue();
    }

    public int getOptionOpenInterest() {
        return valueMap.get(DerivedMktDataField.OPTION_OPEN_INTEREST).intValue();
    }

    public double getTimeValue() {
        return timeValue;
    }

    public double getDelta() {
        return delta;
    }

    public double getGamma() {
        return gamma;
    }
}
