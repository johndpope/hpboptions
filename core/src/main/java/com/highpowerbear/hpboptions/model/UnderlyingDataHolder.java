package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    private final int ibHistDataRequestId;
    private final TreeMap<LocalDate, Double> ivHistoryMap = new TreeMap<>();
    @JsonIgnore
    private final Set<DataField> ivHistoryDependentFields;

    public UnderlyingDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        UnderlyingDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.IV_CHANGE_PCT,
                DerivedMktDataField.IV_RANK,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                UnderlyingDataField.IV_CLOSE,
                UnderlyingDataField.DELTA_CUMULATIVE,
                UnderlyingDataField.GAMMA_CUMULATIVE,
                UnderlyingDataField.DELTA_DOLLARS_CUMULATIVE,
                UnderlyingDataField.TIME_VALUE_CUMULATIVE,
                UnderlyingDataField.UNREALIZED_PL_CUMULATIVE
        ).collect(Collectors.toSet()));

        ivHistoryDependentFields = Stream.of(
                UnderlyingDataField.IV_CLOSE,
                DerivedMktDataField.IV_CHANGE_PCT,
                DerivedMktDataField.IV_RANK
        ).collect(Collectors.toSet());
    }

    @Override
    public void calculateField(DerivedMktDataField field) {
        super.calculateField(field);

        if (field == DerivedMktDataField.IV_CHANGE_PCT) {
            updateIvChangePct();

        } else if (field == DerivedMktDataField.IV_RANK) {
            updateIvRank();

        } else if (field == DerivedMktDataField.OPTION_VOLUME) {
            int o = getCurrent(BasicMktDataField.OPTION_CALL_VOLUME).intValue();
            int p = getCurrent(BasicMktDataField.OPTION_PUT_VOLUME).intValue();

            int value = (isValidSize(o) ? o : 0) + (isValidSize(p) ? p : 0);
            update(field, value);
        }
    }

    public void addImpliedVolatility(LocalDate date, double impliedVolatility) {
        ivHistoryMap.putIfAbsent(date, impliedVolatility);
    }

    public void impliedVolatilityHistoryCompleted() {
        if (!ivHistoryMap.isEmpty()) {
            update(UnderlyingDataField.IV_CLOSE, ivHistoryMap.lastEntry().getValue());

            updateIvChangePct();
            updateIvRank();
        }
    }

    private void updateIvChangePct() {
        if (ivHistoryMap.isEmpty()) {
            return;
        }
        double ivCurrent = getCurrent(BasicMktDataField.OPTION_IMPLIED_VOL).doubleValue();
        double ivClose = ivHistoryMap.lastEntry().getValue();

        if (isValidPrice(ivCurrent) && isValidPrice(ivClose)) {
            ivCurrent = CoreUtil.round(ivCurrent, 8);
            ivClose = CoreUtil.round(ivClose, 8);

            double value = ((ivCurrent - ivClose) / ivClose) * 100d;
            update(DerivedMktDataField.IV_CHANGE_PCT, CoreUtil.round2(value));
        }
    }

    private void updateIvRank() {
        if (ivHistoryMap.isEmpty()) {
            return;
        }
        LocalDate now = LocalDate.now();
        LocalDate yearAgo = now.minusYears(1);

        OptionalDouble ivYearLowOptional = ivHistoryMap.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(yearAgo))
                .mapToDouble(Map.Entry::getValue)
                .min();

        OptionalDouble ivYearHighOptional = ivHistoryMap.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(yearAgo))
                .mapToDouble(Map.Entry::getValue)
                .max();

        if (!ivYearLowOptional.isPresent() || !ivYearHighOptional.isPresent()) {
            return;
        }

        double ivCurrent = getOptionImpliedVol();
        double ivYearLow = ivYearLowOptional.getAsDouble();
        double ivYearHigh = ivYearHighOptional.getAsDouble();

        if (isValidPrice(ivCurrent) && isValidPrice(ivYearLow) && isValidPrice(ivYearHigh)) {
            double ivRank = CoreUtil.round2(100d * (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow));
            update(DerivedMktDataField.IV_RANK, ivRank);
        }
    }

    public void updateCumulativeData(double delta, double gamma, double vega, double theta, double deltaDollars, double timeValue, double unrealizedPl) {
        update(UnderlyingDataField.DELTA_CUMULATIVE, delta);
        update(UnderlyingDataField.GAMMA_CUMULATIVE, gamma);
        update(UnderlyingDataField.VEGA_CUMULATIVE, vega);
        update(UnderlyingDataField.THETA_CUMULATIVE, theta);
        update(UnderlyingDataField.DELTA_DOLLARS_CUMULATIVE, deltaDollars);
        update(UnderlyingDataField.TIME_VALUE_CUMULATIVE, timeValue);
        update(UnderlyingDataField.UNREALIZED_PL_CUMULATIVE, unrealizedPl);
    }

    public Set<DataField> getIvHistoryDependentFields() {
        return ivHistoryDependentFields;
    }

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public double getOptionImpliedVol() {
        return getCurrent(BasicMktDataField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public double getIvClose() {
        return getCurrent(UnderlyingDataField.IV_CLOSE).doubleValue();
    }

    public double getIvChangePct() {
        return getCurrent(DerivedMktDataField.IV_CHANGE_PCT).doubleValue();
    }

    public double getIvRank() {
        return getCurrent(DerivedMktDataField.IV_RANK).doubleValue();
    }

    public int getOptionVolume() {
        return getCurrent(DerivedMktDataField.OPTION_VOLUME).intValue();
    }

    public int getOptionOpenInterest() {
        return getCurrent(DerivedMktDataField.OPTION_OPEN_INTEREST).intValue();
    }

    public double getDeltaCumulative() {
        return getCurrent(UnderlyingDataField.DELTA_CUMULATIVE).doubleValue();
    }

    public double getGammaCumulative() {
        return getCurrent(UnderlyingDataField.GAMMA_CUMULATIVE).doubleValue();
    }

    public double getDeltaDollarsCumulative() {
        return getCurrent(UnderlyingDataField.DELTA_DOLLARS_CUMULATIVE).doubleValue();
    }

    public double getTimeValueCumulative() {
        return getCurrent(UnderlyingDataField.TIME_VALUE_CUMULATIVE).doubleValue();
    }

    public double getUnrealizedPlCumulative() {
        return getCurrent(UnderlyingDataField.UNREALIZED_PL_CUMULATIVE).doubleValue();
    }
}
