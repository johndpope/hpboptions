package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.ib.client.TickType;
import com.ib.client.Types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/6/2018.
 */
public class AbstractOptionDataHolder extends AbstractDataHolder implements OptionDataHolder {

    private final Map<TickType, Map<Computation, Double>> computationMap = new HashMap<>();
    private enum Computation {
        D, G, V, T, IV, OP, UP
    }

    public AbstractOptionDataHolder(DataHolderType type, OptionInstrument instrument, int ibMktDataRequestId) {
        super(type, instrument, ibMktDataRequestId);

        computationMap.put(TickType.BID_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.ASK_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.MODEL_OPTION, new ConcurrentHashMap<>());

        for (Computation c : Computation.values()) {
            computationMap.get(TickType.BID_OPTION).put(c, Double.NaN);
            computationMap.get(TickType.ASK_OPTION).put(c, Double.NaN);
            computationMap.get(TickType.MODEL_OPTION).put(c, Double.NaN);
        }

        OptionDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                OptionDataField.DELTA,
                OptionDataField.GAMMA,
                OptionDataField.IMPLIED_VOL,
                OptionDataField.TIME_VALUE,
                OptionDataField.TIME_VALUE_PCT
        ).collect(Collectors.toSet()));
    }

    @Override
    public OptionInstrument getInstrument() {
        return (OptionInstrument) instrument;
    }

    @Override
    public void updateOptionData(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVol, double optionPrice, double underlyingPrice) {
        Map<Computation, Double> m = computationMap.get(tickType);

        m.put(Computation.D, delta);
        m.put(Computation.G, gamma);
        m.put(Computation.V, vega);
        m.put(Computation.T, theta);
        m.put(Computation.IV, impliedVol);
        m.put(Computation.OP, optionPrice);
        m.put(Computation.UP, underlyingPrice);

        if (tickType != TickType.MODEL_OPTION) { // store on bid, ask or model, but recalculate only on model
            return;
        }

        double greekIntpl = interpolate(Computation.D);
        if (valid(greekIntpl)) {
            update(OptionDataField.DELTA, greekIntpl);
        }

        greekIntpl = interpolate(Computation.G);
        if (valid(greekIntpl)) {
            update(OptionDataField.GAMMA, greekIntpl);
        }

        greekIntpl = interpolate(Computation.V);
        if (valid(greekIntpl)) {
            update(OptionDataField.VEGA, greekIntpl);
        }

        greekIntpl = interpolate(Computation.T);
        if (valid(greekIntpl)) {
            update(OptionDataField.THETA, greekIntpl);
        }

        update(OptionDataField.IMPLIED_VOL, interpolate(Computation.IV));

        double optionPriceIntpl = interpolate(Computation.OP);
        double underlyingPriceIntpl = interpolate(Computation.UP);

        if (valid(optionPrice) && valid(underlyingPriceIntpl)) {
            double timeValue = optionPriceIntpl - intrinsicValue(underlyingPriceIntpl);
            double timeValuePct = (timeValue / getInstrument().getStrike()) * (365 / (double) getDaysToExpiration()) * 100d;

            update(OptionDataField.OPTION_PRICE, optionPriceIntpl);
            update(OptionDataField.UNDERLYING_PRICE, underlyingPriceIntpl);
            update(OptionDataField.TIME_VALUE, CoreUtil.round4(timeValue));
            update(OptionDataField.TIME_VALUE_PCT, CoreUtil.round4(timeValuePct));
        }
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

    private double interpolate(Computation c) {
        double cb = computationMap.get(TickType.BID_OPTION).get(c);
        double ca = computationMap.get(TickType.ASK_OPTION).get(c);
        double cm = computationMap.get(TickType.MODEL_OPTION).get(c);

        return valid(cb) && valid(ca) ?
                (cb + ca) / 2d :
                (valid(cm) ? cm : Double.NaN);
    }

    private boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value != Double.MAX_VALUE;
    }

    @Override
    public boolean portfolioSourceFieldsReady() {
        return OptionDataField.portfolioSourceFields().stream().allMatch(field -> valid(getCurrent(field).doubleValue()));
    }

    public long getDaysToExpiration() {
        return ChronoUnit.DAYS.between(LocalDate.now(), getInstrument().getExpirationDate());
    }

    public int getOptionOpenInterest() {
        return getCurrent(DerivedMktDataField.OPTION_OPEN_INTEREST).intValue();
    }

    public double getDelta() {
        return getCurrent(OptionDataField.DELTA).doubleValue();
    }

    public double getGamma() {
        return getCurrent(OptionDataField.GAMMA).doubleValue();
    }

    public double getVega() {
        return getCurrent(OptionDataField.VEGA).doubleValue();
    }

    public double getTheta() {
        return getCurrent(OptionDataField.THETA).doubleValue();
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

    public double getOptionPrice() {
        return getCurrent(OptionDataField.OPTION_PRICE).doubleValue();
    }

    public double getUnderlyingPrice() {
        return getCurrent(OptionDataField.UNDERLYING_PRICE).doubleValue();
    }
}
