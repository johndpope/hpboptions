package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.enums.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class UnderlyingDataHolder extends AbstractDataHolder {

    private final Instrument cfdInstrument;
    private final int ibHistDataRequestId;
    private final int ibPnlRequestId;
    private final TreeMap<LocalDate, Double> ivHistoryMap = new TreeMap<>();
    @JsonIgnore
    private final Set<DataField> ivHistoryDependentFields;

    private Map<DataField, Double> thresholdBreachedFieldsMap = new HashMap<>();
    @JsonIgnore
    private final ReentrantLock riskCalculationLock = new ReentrantLock();
    @JsonIgnore
    private final ReentrantLock pnlCalculationLock = new ReentrantLock();

    public UnderlyingDataHolder(Instrument instrument, Instrument cfdInstrument, int ibMktDataRequestId, int ibHistDataRequestId, int ibPnlRequestId) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId);

        this.cfdInstrument = cfdInstrument;
        this.ibHistDataRequestId = ibHistDataRequestId;
        this.ibPnlRequestId = ibPnlRequestId;

        UnderlyingDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                DerivedMktDataField.CHANGE_PCT,
                BasicMktDataField.OPTION_IMPLIED_VOL,
                DerivedMktDataField.IV_CHANGE_PCT,
                DerivedMktDataField.IV_RANK,
                DerivedMktDataField.OPTION_VOLUME,
                DerivedMktDataField.OPTION_OPEN_INTEREST,
                UnderlyingDataField.CFD_POSITION_SIZE,
                UnderlyingDataField.CFD_UNREALIZED_PNL,
                UnderlyingDataField.IV_CLOSE,
                UnderlyingDataField.PUTS_SUM,
                UnderlyingDataField.CALLS_SUM,
                UnderlyingDataField.PORTFOLIO_DELTA,
                UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT,
                UnderlyingDataField.PORTFOLIO_GAMMA,
                UnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT,
                UnderlyingDataField.PORTFOLIO_VEGA,
                UnderlyingDataField.PORTFOLIO_THETA,
                UnderlyingDataField.PORTFOLIO_TIME_VALUE,
                UnderlyingDataField.ALLOCATION_PCT,
                UnderlyingDataField.PORTFOLIO_UNREALIZED_PNL
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

    public void updateCfdPositionSize(int cfdPositionSize) {
        update(UnderlyingDataField.CFD_POSITION_SIZE, cfdPositionSize);

        UnderlyingDataField cfdMarginField = UnderlyingDataField.CFD_MARGIN;
        if (cfdPositionSize == 0) {
            reset(cfdMarginField);

        } else if (HopUtil.isValidPrice(getLast())) {
            double cfdMargin = Math.abs(cfdPositionSize) * getLast() * HopSettings.CFD_MARGIN_FACTOR;
            update(cfdMarginField, cfdMargin);
        }
    }

    public void updateCfdUnrealizedPnl(double unrealizedPnl) {
        update(UnderlyingDataField.CFD_UNREALIZED_PNL, unrealizedPnl);
    }

    public void resetCfdFields() {
        UnderlyingDataField.cfdFields().forEach(this::reset);
    }

    public void updatePositionsSum(int putsSum, int callsSum) {
        update(UnderlyingDataField.PUTS_SUM, putsSum);
        update(UnderlyingDataField.CALLS_SUM, callsSum);
    }

    public void resetPositionsSum() {
        Stream.of(UnderlyingDataField.PUTS_SUM, UnderlyingDataField.CALLS_SUM).forEach(this::reset);
    }

    public ReentrantLock getRiskCalculationLock() {
        return riskCalculationLock;
    }

    public ReentrantLock getPnlCalculationLock() {
        return pnlCalculationLock;
    }

    public void updateRiskData(double delta, double deltaOnePct, double gamma, double gammaOnePctPct, double vega, double theta, double timeValue, double margin, double allocationPct) {
        update(UnderlyingDataField.PORTFOLIO_DELTA, delta);
        update(UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, deltaOnePct);
        update(UnderlyingDataField.PORTFOLIO_GAMMA, gamma);
        update(UnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT, gammaOnePctPct);
        update(UnderlyingDataField.PORTFOLIO_VEGA, vega);
        update(UnderlyingDataField.PORTFOLIO_THETA, theta);
        update(UnderlyingDataField.PORTFOLIO_TIME_VALUE, timeValue);
        update(UnderlyingDataField.PORTFOLIO_MARGIN, margin);
        update(UnderlyingDataField.ALLOCATION_PCT, allocationPct);

        populateThresholdBreachedFieldsMap();
    }

    public void updateCfdOnlyRiskData(double delta, double deltaOnePct, double margin, double allocationPct) {
        update(UnderlyingDataField.PORTFOLIO_DELTA, delta);
        update(UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, deltaOnePct);
        update(UnderlyingDataField.PORTFOLIO_MARGIN, margin);
        update(UnderlyingDataField.ALLOCATION_PCT, allocationPct);

        populateThresholdBreachedFieldsMap();
    }

    private void populateThresholdBreachedFieldsMap() {
        thresholdBreachedFieldsMap.clear();
        for (UnderlyingDataField f : UnderlyingDataField.riskDataFields()) {
            double current = getCurrent(f).doubleValue();
            if (f.thresholdBreached(current)) {
                thresholdBreachedFieldsMap.put(f, current);
            }
        }
    }

    public void resetRiskData() {
        UnderlyingDataField.riskDataFields().forEach(this::reset);
    }

    public void updatePortfolioUnrealizedPnl(double unrealizedPnl) {
        update(UnderlyingDataField.PORTFOLIO_UNREALIZED_PNL, unrealizedPnl);
    }

    public void resetPortfolioUnrealizedPnl() {
        reset(UnderlyingDataField.PORTFOLIO_UNREALIZED_PNL);
    }

    public Set<DataField> getIvHistoryDependentFields() {
        return ivHistoryDependentFields;
    }

    public Instrument getCfdInstrument() {
        return cfdInstrument;
    }

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public Map<DataField, Double> getThresholdBreachedFieldsMap() {
        return thresholdBreachedFieldsMap;
    }

    public double getOptionImpliedVol() {
        return getCurrent(BasicMktDataField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public int getCfdPositionSize() {
        return getCurrent(UnderlyingDataField.CFD_POSITION_SIZE).intValue();
    }

    public double getCfdUnrealizedPnl() {
        return getCurrent(UnderlyingDataField.CFD_UNREALIZED_PNL).doubleValue();
    }

    public double getCfdMargin() {
        return getCurrent(UnderlyingDataField.CFD_MARGIN).doubleValue();
    }

    public double getIvClose() {
        return getCurrent(UnderlyingDataField.IV_CLOSE).doubleValue();
    }

    public int getPutsSum() {
        return getCurrent(UnderlyingDataField.PUTS_SUM).intValue();
    }

    public int getCallsSum() {
        return getCurrent(UnderlyingDataField.CALLS_SUM).intValue();
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

    public double getPortfolioGammaOnePctPct() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT).doubleValue();
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

    public double getPortfolioUnrealizedPnl() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_UNREALIZED_PNL).doubleValue();
    }

    public double getPortfolioMargin() {
        return getCurrent(UnderlyingDataField.PORTFOLIO_MARGIN).doubleValue();
    }

    public double getAllocationPct() {
        return getCurrent(UnderlyingDataField.ALLOCATION_PCT).doubleValue();
    }
}
