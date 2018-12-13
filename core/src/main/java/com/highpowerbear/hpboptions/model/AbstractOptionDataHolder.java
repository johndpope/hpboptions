package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.ib.client.Types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/6/2018.
 */
public class AbstractOptionDataHolder extends AbstractDataHolder implements OptionDataHolder {

    private Types.Right right;
    private double strike;
    private LocalDate expirationDate;

    public AbstractOptionDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId, Types.Right right, double strike, LocalDate expirationDate) {
        super(type, instrument, ibMktDataRequestId);
        this.right = right;
        this.strike = strike;
        this.expirationDate = expirationDate;

        OptionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                OptionDataField.DELTA,
                OptionDataField.GAMMA,
                OptionDataField.IMPLIED_VOL,
                OptionDataField.TIME_VALUE,
                OptionDataField.TIME_VALUE_PCT
        ).collect(Collectors.toSet()));
    }

    @Override
    public void updateOptionData(double delta, double gamma, double vega, double theta, double impliedVol, double optionPrice, double underlyingPrice) {
        // TODO calculate timeValue and timeValuePct
        double timeValue = 0;
        double timeValuePct = 0;

        update(OptionDataField.DELTA, delta);
        update(OptionDataField.GAMMA, gamma);
        update(OptionDataField.VEGA, vega);
        update(OptionDataField.THETA, theta);
        update(OptionDataField.IMPLIED_VOL, impliedVol);
        update(OptionDataField.OPTION_PRICE, optionPrice);
        update(OptionDataField.UNDERLYING_PRICE, underlyingPrice);

        update(OptionDataField.TIME_VALUE, timeValue);
        update(OptionDataField.TIME_VALUE_PCT, timeValuePct);
    }

    public Types.Right getRight() {
        return right;
    }

    public double getStrike() {
        return strike;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public long getDaysToExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    public double getDelta() {
        return getCurrent(OptionDataField.DELTA).doubleValue();
    }

    public double getGamma() {
        return getCurrent(OptionDataField.GAMMA).doubleValue();
    }

    public double getImpliedVol() {
        return getCurrent(OptionDataField.IMPLIED_VOL).doubleValue();
    }

    public double getTimeValue() {
        return getCurrent(OptionDataField.TIME_VALUE).doubleValue();
    }

    public double getTimeValuePct() {
        return getCurrent(OptionDataField.TIME_VALUE_PCT).doubleValue();
    }
}
