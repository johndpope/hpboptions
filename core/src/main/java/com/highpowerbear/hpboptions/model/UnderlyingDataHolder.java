package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    private CumulativeOptionData cumulativeOptionData = new CumulativeOptionData();
    private double cumulativeUnrealizedPl;

    public UnderlyingDataHolder(Instrument instrument, int ibRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibRequestId);

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));
    }

    public void updateCumulativeOptionData(double delta, double gamma, double vega, double theta, double deltaDollars, double timeValue) {
        cumulativeOptionData.update(delta, gamma, vega, theta, deltaDollars, timeValue);
    }

    public void updateCumulativeUnrealizedPl(double pl) {
        cumulativeUnrealizedPl = pl;
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

    public CumulativeOptionData getCumulativeOptionData() {
        return cumulativeOptionData;
    }

    public double getCumulativeUnrealizedPl() {
        return cumulativeUnrealizedPl;
    }
}
