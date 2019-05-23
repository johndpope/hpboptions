package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.DerivedMktDataField;
import com.highpowerbear.hpboptions.field.OptionDataField;
import com.highpowerbear.hpboptions.model.OptionInstrument;
import com.ib.client.TickType;
import com.ib.client.Types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.highpowerbear.hpboptions.field.OptionDataField.*;

/**
 * Created by robertk on 12/6/2018.
 */
public abstract class AbstractOptionDataHolder extends AbstractMarketDataHolder implements OptionDataHolder {

    private final Map<TickType, Map<OptionDataField, Double>> computationMap = new HashMap<>();

    public AbstractOptionDataHolder(DataHolderType type, OptionInstrument instrument, int ibMktDataRequestId) {
        super(type, instrument, ibMktDataRequestId);

        computationMap.put(TickType.BID_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.ASK_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.MODEL_OPTION, new ConcurrentHashMap<>());

        for (OptionDataField field : OptionDataField.values()) {
            computationMap.get(TickType.BID_OPTION).put(field, Double.NaN);
            computationMap.get(TickType.ASK_OPTION).put(field, Double.NaN);
            computationMap.get(TickType.MODEL_OPTION).put(field, Double.NaN);
        }

        OptionDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                DerivedMktDataField.CHANGE,
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                DELTA,
                GAMMA,
                IMPLIED_VOL,
                INTRINSIC_VALUE,
                TIME_VALUE,
                TIME_VALUE_PCT
        ).collect(Collectors.toSet()));
    }

    @Override
    public OptionInstrument getInstrument() {
        return (OptionInstrument) instrument;
    }

    @Override
    public void optionDataReceived(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVol, double optionPrice, double underlyingPrice) {
        Map<OptionDataField, Double> m = computationMap.get(tickType);

        m.put(DELTA, delta);
        m.put(GAMMA, gamma);
        m.put(VEGA, vega);
        m.put(THETA, theta);
        m.put(IMPLIED_VOL, impliedVol);
        m.put(OPTION_PRICE, optionPrice);
        m.put(UNDERLYING_PRICE, underlyingPrice);
    }

    @Override
    public void recalculateOptionData() {
        Stream.of(DELTA, GAMMA, VEGA, THETA, IMPLIED_VOL).forEach(field -> {
            double value = interpolate(field);
            if (valid(value)) {
                update(field, value);
            }
        });

        double optionPriceIntpl = interpolate(OPTION_PRICE);
        double underlyingPriceIntpl = interpolate(UNDERLYING_PRICE);

        if (valid(optionPriceIntpl) && valid(underlyingPriceIntpl)) {
            double atmDistancePct = atmDistancePct(underlyingPriceIntpl);
            double intrinsicValue = intrinsicValue(underlyingPriceIntpl);
            double timeValue = optionPriceIntpl - intrinsicValue;

            double timeValuePct = getDaysToExpiration() > 0 ?
                    (timeValue / getInstrument().getStrike()) * (365 / (double) getDaysToExpiration()) * 100d :
                    Double.NaN;

            update(OPTION_PRICE, optionPriceIntpl);
            update(UNDERLYING_PRICE, underlyingPriceIntpl);
            update(ATM_DISTANCE_PCT, atmDistancePct);
            update(INTRINSIC_VALUE, intrinsicValue);
            update(TIME_VALUE, timeValue);
            update(TIME_VALUE_PCT, timeValuePct);
        }
    }

    private double atmDistancePct(double underlyingPrice) {
        Types.Right right = getInstrument().getRight();
        double strike = getInstrument().getStrike();

        double atmDistancePct = ((strike - underlyingPrice) / strike) * 100d;
        if (right == Types.Right.Put) {
            atmDistancePct *= -1d;
        }
        return atmDistancePct;
    }

    private double intrinsicValue(double underlyingPrice) {
        Types.Right right = getInstrument().getRight();
        double strike = getInstrument().getStrike();

        if (right == Types.Right.Call && underlyingPrice > strike) {
            return underlyingPrice - strike;
        } else if (right == Types.Right.Put && underlyingPrice < strike) {
            return strike - underlyingPrice;
        } else {
            return 0d;
        }
    }

    private double interpolate(OptionDataField field) {
        double computationBid = computationMap.get(TickType.BID_OPTION).get(field);
        double computationAsk = computationMap.get(TickType.ASK_OPTION).get(field);
        double computationModel = computationMap.get(TickType.MODEL_OPTION).get(field);

        return valid(computationBid) && valid(computationAsk) ?
                (computationBid + computationAsk) / 2d :
                (valid(computationModel) ? computationModel : Double.NaN);
    }

    private boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value != Double.MAX_VALUE;
    }

    public boolean riskDataSourceFieldsReady() {
        return OptionDataField.riskDataSourceFields().stream().allMatch(field -> valid(getCurrent(field).doubleValue()));
    }

    public long getDaysToExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), getInstrument().getExpiration());
    }

    public int getOptionOpenInterest() {
        return getCurrent(DerivedMktDataField.OPTION_OPEN_INTEREST).intValue();
    }

    public double getDelta() {
        return getCurrent(DELTA).doubleValue();
    }

    public double getGamma() {
        return getCurrent(GAMMA).doubleValue();
    }

    public double getVega() {
        return getCurrent(VEGA).doubleValue();
    }

    public double getTheta() {
        return getCurrent(THETA).doubleValue();
    }

    public double getImpliedVol() {
        return getCurrent(IMPLIED_VOL).doubleValue();
    }

    public double getAtmDistancePct() {
        return getCurrent(ATM_DISTANCE_PCT).doubleValue();
    }

    public double getIntrinsicValue() {
        return getCurrent(INTRINSIC_VALUE).doubleValue();
    }

    public double getTimeValue() {
        return getCurrent(TIME_VALUE).doubleValue();
    }

    public double getTimeValuePct() {
        return getCurrent(TIME_VALUE_PCT).doubleValue();
    }

    public double getOptionPrice() {
        return getCurrent(OPTION_PRICE).doubleValue();
    }

    public double getUnderlyingPrice() {
        return getCurrent(UNDERLYING_PRICE).doubleValue();
    }
}
