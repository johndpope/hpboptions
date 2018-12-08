package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.ib.client.Bar;

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
    private final SortedMap<LocalDate, Double> impliedVolatilityHistoryMap = new TreeMap<>();
    private double impliedVolatilityRank = Double.NaN;

    private CumulativeOptionData cumulativeOptionData = new CumulativeOptionData();
    private double cumulativeUnrealizedPl;

    public UnderlyingDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        addFieldsToDisplay(Stream.of(
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));
    }

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public void addImpliedVolatilityHistoricValue(Bar bar) {
        LocalDate date = CoreUtil.toLocalDate(Long.valueOf(bar.time()));
        double impliedVolatility = bar.close();

        impliedVolatilityHistoryMap.putIfAbsent(date, impliedVolatility);
    }

    public void calculateImpliedVolatilityRank() {
        LocalDate now = LocalDate.now();
        LocalDate yearAgo = now.minusYears(1);

        OptionalDouble ivYearLowOptional = impliedVolatilityHistoryMap.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(yearAgo))
                .mapToDouble(Map.Entry::getValue)
                .min();

        OptionalDouble ivYearHighOptional = impliedVolatilityHistoryMap.entrySet().stream()
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
            double rank = (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow);
            impliedVolatilityRank = Double.parseDouble(String.format("%.1f", rank * 100d));
        }
    }

    public void updateCumulativeOptionData(double delta, double gamma, double vega, double theta, double deltaDollars, double timeValue) {
        cumulativeOptionData.update(delta, gamma, vega, theta, deltaDollars, timeValue);
    }

    public double getImpliedVolatilityRank() {
        return impliedVolatilityRank;
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
