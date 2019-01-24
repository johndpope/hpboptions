package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.enums.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
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

    private long lastRiskUpdateTime;

    public UnderlyingDataHolder(Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        UnderlyingDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                DerivedMktDataField.CHANGE_PCT,
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.IV_CHANGE_PCT,
                DerivedMktDataField.IV_RANK,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                UnderlyingDataField.IV_CLOSE,
                UnderlyingDataField.PORTFOLIO_DELTA,
                UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT,
                UnderlyingDataField.PORTFOLIO_GAMMA,
                UnderlyingDataField.PORTFOLIO_VEGA,
                UnderlyingDataField.PORTFOLIO_THETA,
                UnderlyingDataField.PORTFOLIO_TIME_VALUE,
                UnderlyingDataField.ALLOCATION_PCT,
                UnderlyingDataField.UNREALIZED_PNL
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
            double value = ((ivCurrent - ivClose) / ivClose) * 100d;
            update(DerivedMktDataField.IV_CHANGE_PCT, value);
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
            double ivRank = 100d * (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow);
            update(DerivedMktDataField.IV_RANK, ivRank);
        }
    }

    public void updateRiskData(double delta, double deltaOnePct, double gamma, double vega, double theta, double timeValue, double allocationPct) {
        lastRiskUpdateTime = System.currentTimeMillis();

        update(UnderlyingDataField.PORTFOLIO_DELTA, delta);
        update(UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, deltaOnePct);
        update(UnderlyingDataField.PORTFOLIO_GAMMA, gamma);
        update(UnderlyingDataField.PORTFOLIO_VEGA, vega);
        update(UnderlyingDataField.PORTFOLIO_THETA, theta);
        update(UnderlyingDataField.PORTFOLIO_TIME_VALUE, timeValue);
        update(UnderlyingDataField.ALLOCATION_PCT, allocationPct);
    }

    public void resetRiskData() {
        lastRiskUpdateTime = 0;
        UnderlyingDataField.riskDataFields().forEach(field -> update(field, field.getInitialValue()));
    }

    public boolean isRiskDataUpdateDue() {
        return (System.currentTimeMillis() - lastRiskUpdateTime) > HopSettings.RISK_DATA_UPDATE_INTERVAL_MILLIS;
    }

    public void updateUnrealizedPnl(double unrealizedPnl) {
        update(UnderlyingDataField.UNREALIZED_PNL, unrealizedPnl);
    }

    public void resetUnrealizedPnl() {
        UnderlyingDataField field = UnderlyingDataField.UNREALIZED_PNL;
        update(field, field.getInitialValue());
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

    public double getPortfolioDelta() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_DELTA).doubleValue();
    }

    public double getPortfolioDeltaOnePct() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT).doubleValue();
    }

    public double getPortfolioGamma() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_GAMMA).doubleValue();
    }

    public double getPortfolioVega() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_VEGA).doubleValue();
    }

    public double getPortfolioTheta() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_THETA).doubleValue();
    }

    public double getPortfolioTimeValue() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_TIME_VALUE).doubleValue();
    }

    public double getAllocationPct() {
        return getCurrent(UnderlyingDataField.ALLOCATION_PCT).doubleValue();
    }

    public double getUnrealizedPnl() {
        return getCurrent(UnderlyingDataField.UNREALIZED_PNL).doubleValue();
    }
}
