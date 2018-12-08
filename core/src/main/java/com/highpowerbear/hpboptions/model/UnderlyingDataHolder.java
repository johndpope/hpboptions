package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;

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
    private double ivRank = Double.NaN, ivRankOld = Double.NaN;
    private CumulativeData cumulativeData = new CumulativeData();

    private final SortedMap<LocalDate, Double> ivHistoryMap = new TreeMap<>();

    public UnderlyingDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));
    }

    public void addImpliedVolatilityHistoricValue(LocalDate date, double impliedVolatility) {
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
            double value = 100d * (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow);
            ivRankOld = ivRank;
            ivRank = CoreUtil.round(value, 1);
        }
    }

    public void updateCumulativeData(double delta, double gamma, double vega, double theta, double deltaDollars, double timeValue, double unrealizedPl) {
        cumulativeData.update(delta, gamma, vega, theta, deltaDollars, timeValue, unrealizedPl);
    }

    public String createIvRankMessage() {
        return id + ",ivRank," + ivRankOld + "," + ivRank;
    }

    public String createCumulativeDataMessage() {
        return id + "," + cumulativeData.toCsvString();
    }

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public double getIvRank() {
        return ivRank;
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

    public CumulativeData getCumulativeData() {
        return cumulativeData;
    }
}
