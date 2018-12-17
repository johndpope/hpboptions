package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.ib.client.TickType;
import com.ib.client.Types;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/6/2018.
 */
public class AbstractOptionDataHolder extends AbstractDataHolder implements OptionDataHolder {

    private final Types.Right right;
    private final double strike;
    private final LocalDate expirationDate;

    private final Map<TickType, Map<Computation, Double>> computationMap = new HashMap<>();

    private enum Computation {
        D, G, V, T, IV, OP, UP
    }

    public AbstractOptionDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId, Types.Right right, double strike, LocalDate expirationDate) {
        super(type, instrument, ibMktDataRequestId);
        this.right = right;
        this.strike = strike;
        this.expirationDate = expirationDate;

        computationMap.put(TickType.BID_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.ASK_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.LAST_OPTION, new ConcurrentHashMap<>());
        computationMap.put(TickType.MODEL_OPTION, new ConcurrentHashMap<>());

        computationMap.values().forEach(m -> Arrays.asList(Computation.values()).forEach(c -> m.put(c, Double.NaN)));
        OptionDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

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
    public void updateOptionData(TickType tickType, double delta, double gamma, double vega, double theta, double impliedVol, double optionPrice, double underlyingPrice) {
        Map<Computation, Double> m = computationMap.get(tickType);

        m.put(Computation.D, delta);
        m.put(Computation.G, gamma);
        m.put(Computation.V, vega);
        m.put(Computation.T, theta);
        m.put(Computation.IV, impliedVol);
        m.put(Computation.OP, optionPrice);
        m.put(Computation.UP, underlyingPrice);

        update(OptionDataField.DELTA, interpolate(Computation.D));
        update(OptionDataField.GAMMA, interpolate(Computation.G));
        update(OptionDataField.VEGA, interpolate(Computation.V));
        update(OptionDataField.THETA, interpolate(Computation.T));
        update(OptionDataField.IMPLIED_VOL, interpolate(Computation.IV));

        double timeValue = timeValue(interpolate(Computation.OP), interpolate(Computation.UP));
        double timeValuePct = timeValuePct(timeValue);

        update(OptionDataField.TIME_VALUE, CoreUtil.round2(timeValue));
        update(OptionDataField.TIME_VALUE_PCT, CoreUtil.round2(timeValuePct));
    }

    private double timeValue(double optionPrice, double underlyingPrice) {
        if (isValidPrice(optionPrice) && isValidPrice(underlyingPrice)) {
            return optionPrice - intrinsicValue(underlyingPrice);
        } else {
            return Double.NaN;
        }
    }

    private double timeValuePct(double timeValue) {
        if (valid(timeValue)) {
            return (timeValue / strike) * (365 / (double) getDaysToExpiration()) * 100d;
        } else {
            return Double.NaN;
        }
    }

    private double intrinsicValue(double underlyingPrice) {
        if (right == Types.Right.Call && underlyingPrice > strike) {
            return underlyingPrice - strike;
        } else if (right == Types.Right.Put && underlyingPrice < strike) {
            return strike - underlyingPrice;
        } else {
            return 0d;
        }
    }

    private double interpolate(Computation c) {
        double b = getBid();
        double a = getAsk();
        double l = getLast();

        double db = computationMap.get(TickType.BID_OPTION).get(c);
        double da = computationMap.get(TickType.ASK_OPTION).get(c);
        double dl = computationMap.get(TickType.LAST_OPTION).get(c);
        double dm = computationMap.get(TickType.MODEL_OPTION).get(c);

        if (isValidPrice(b) && isValidPrice(a) && valid(db) && valid(da)) {
            return (db + da) / 2d;
        } else if (isValidPrice(b) && isValidPrice(l) && valid(db) && valid(dl)) {
            return l > b ? dl : db;
        } else if (isValidPrice(a) && isValidPrice(l) && valid(da) && valid(dl)) {
            return l < a ? dl : da;
        } else if (isValidPrice(l) && valid(dl)) {
            return dl;
        } else if (isValidPrice(b) && valid(db)) {
            return db;
        } else if (isValidPrice(a) && valid(da)) {
            return da;
        } else if (valid(dm)) {
            return dm;
        } else {
            return Double.NaN;
        }
    }

    private boolean valid(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value != Double.MAX_VALUE;
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
}
