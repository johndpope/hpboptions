package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.UnderlyingDataField;

import java.time.LocalDate;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    private final int ibHistDataRequestId;
    private final SortedMap<LocalDate, Double> ivHistoryMap = new TreeMap<>();

    public UnderlyingDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        UnderlyingDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                UnderlyingDataField.IV_RANK,
                UnderlyingDataField.DELTA_CUMULATIVE,
                UnderlyingDataField.GAMMA_CUMULATIVE,
                UnderlyingDataField.DELTA_DOLLARS_CUMULATIVE,
                UnderlyingDataField.TIME_VALUE_CUMULATIVE,
                UnderlyingDataField.UNREALIZED_PL_CUMULATIVE
        ).collect(Collectors.toSet()));
    }

    public void addImpliedVolatility(LocalDate date, double impliedVolatility) {
        ivHistoryMap.putIfAbsent(date, impliedVolatility);
    }

    public void updateIvRank() {
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
            double ivRank = CoreUtil.round(100d * (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow), 1);
            update(UnderlyingDataField.IV_RANK, ivRank);
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

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public double getOptionImpliedVol() {
        return getCurrent(BasicMktDataField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public int getOptionVolume() {
        return getCurrent(DerivedMktDataField.OPTION_VOLUME).intValue();
    }

    public int getOptionOpenInterest() {
        return getCurrent(DerivedMktDataField.OPTION_OPEN_INTEREST).intValue();
    }

    public double getIvRank() {
        return getCurrent(UnderlyingDataField.IV_RANK).doubleValue();
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
