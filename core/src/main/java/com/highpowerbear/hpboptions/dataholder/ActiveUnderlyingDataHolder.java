package com.highpowerbear.hpboptions.dataholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.ActiveUnderlyingDataField;
import com.highpowerbear.hpboptions.field.DerivedMktDataField;
import com.highpowerbear.hpboptions.model.Instrument;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/3/2018.
 */
public class ActiveUnderlyingDataHolder extends AbstractUnderlyingDataHolder {

    private final Instrument cfdInstrument;
    private final int ibPnlRequestId;

    private boolean deltaHedge = false;
    private LocalDateTime lastDeltaHedgeTime;
    private final LocalTime marketOpen;
    private final LocalTime marketClose;

    @JsonIgnore
    private final ReentrantLock riskCalculationLock = new ReentrantLock();
    @JsonIgnore
    private final ReentrantLock pnlCalculationLock = new ReentrantLock();

    public ActiveUnderlyingDataHolder(Instrument instrument, Instrument cfdInstrument, int ibMktDataRequestId, int ibHistDataRequestId, int ibPnlRequestId, LocalTime marketOpen, LocalTime marketClose) {
        super(DataHolderType.UNDERLYING, instrument, ibMktDataRequestId, ibHistDataRequestId);

        this.cfdInstrument = cfdInstrument;
        this.ibPnlRequestId = ibPnlRequestId;
        this.marketOpen = marketOpen;
        this.marketClose = marketClose;

        ActiveUnderlyingDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                ActiveUnderlyingDataField.CFD_POSITION_SIZE,
                ActiveUnderlyingDataField.CFD_UNREALIZED_PNL,
                ActiveUnderlyingDataField.PUTS_SHORT,
                ActiveUnderlyingDataField.PUTS_LONG,
                ActiveUnderlyingDataField.CALLS_SHORT,
                ActiveUnderlyingDataField.CALLS_LONG,
                ActiveUnderlyingDataField.PORTFOLIO_DELTA,
                ActiveUnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT,
                ActiveUnderlyingDataField.PORTFOLIO_GAMMA,
                ActiveUnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT,
                ActiveUnderlyingDataField.PORTFOLIO_VEGA,
                ActiveUnderlyingDataField.PORTFOLIO_THETA,
                ActiveUnderlyingDataField.PORTFOLIO_TIME_VALUE,
                ActiveUnderlyingDataField.ALLOCATION_PCT,
                ActiveUnderlyingDataField.PORTFOLIO_UNREALIZED_PNL
        ).collect(Collectors.toSet()));

        updateLastDeltaHedgeTime();
    }

    public void updateCfdPositionSize(int cfdPositionSize) {
        update(ActiveUnderlyingDataField.CFD_POSITION_SIZE, cfdPositionSize);

        ActiveUnderlyingDataField cfdMarginField = ActiveUnderlyingDataField.CFD_MARGIN;
        if (cfdPositionSize == 0) {
            reset(cfdMarginField);

        } else if (HopUtil.isValidPrice(getLast())) {
            double cfdMargin = Math.abs(cfdPositionSize) * getLast() * HopSettings.CFD_MARGIN_FACTOR;
            update(cfdMarginField, cfdMargin);
        }
    }

    public void updateCfdUnrealizedPnl(double unrealizedPnl) {
        update(ActiveUnderlyingDataField.CFD_UNREALIZED_PNL, unrealizedPnl);
    }

    public void resetCfdFields() {
        ActiveUnderlyingDataField.cfdFields().forEach(this::reset);
    }

    public void updateOptionPositionsSum(int putsShort, int putsLong, int callsShort, int callsLong) {
        update(ActiveUnderlyingDataField.PUTS_SHORT, putsShort);
        update(ActiveUnderlyingDataField.PUTS_LONG, putsLong);
        update(ActiveUnderlyingDataField.CALLS_SHORT, callsShort);
        update(ActiveUnderlyingDataField.CALLS_LONG, callsLong);
    }

    public void resetOptionPositionsSum() {
        ActiveUnderlyingDataField.optionPositionSumFields().forEach(this::reset);
    }

    public ReentrantLock getRiskCalculationLock() {
        return riskCalculationLock;
    }

    public ReentrantLock getPnlCalculationLock() {
        return pnlCalculationLock;
    }

    public void updateRiskData(double delta, double deltaOnePct, double gamma, double gammaOnePctPct, double vega, double theta, double timeValue, double margin, double allocationPct) {
        update(ActiveUnderlyingDataField.PORTFOLIO_DELTA, delta);
        update(ActiveUnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, deltaOnePct);
        update(ActiveUnderlyingDataField.PORTFOLIO_GAMMA, gamma);
        update(ActiveUnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT, gammaOnePctPct);
        update(ActiveUnderlyingDataField.PORTFOLIO_VEGA, vega);
        update(ActiveUnderlyingDataField.PORTFOLIO_THETA, theta);
        update(ActiveUnderlyingDataField.PORTFOLIO_TIME_VALUE, timeValue);
        update(ActiveUnderlyingDataField.PORTFOLIO_MARGIN, margin);
        update(ActiveUnderlyingDataField.ALLOCATION_PCT, allocationPct);
    }

    public void updateCfdOnlyRiskData(double delta, double deltaOnePct, double margin, double allocationPct) {
        update(ActiveUnderlyingDataField.PORTFOLIO_DELTA, delta);
        update(ActiveUnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, deltaOnePct);
        update(ActiveUnderlyingDataField.PORTFOLIO_MARGIN, margin);
        update(ActiveUnderlyingDataField.ALLOCATION_PCT, allocationPct);
    }

    public void resetRiskData() {
        ActiveUnderlyingDataField.riskDataFields().forEach(this::reset);
    }

    public boolean isDeltaHedge() {
        return deltaHedge;
    }

    public void setDeltaHedge(boolean deltaHedge) {
        this.deltaHedge = deltaHedge;
    }

    public void updateLastDeltaHedgeTime() {
        lastDeltaHedgeTime = LocalDateTime.now();
    }

    public boolean isDeltaHedgeDue() {
        return LocalDateTime.now().isAfter(lastDeltaHedgeTime.plusSeconds(HopSettings.DELTA_HEDGE_MIN_INTERVAL_SEC));
    }

    public boolean isMarketOpen() {
        LocalTime time = LocalTime.now();
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        return time.isAfter(marketOpen) && time.isBefore(marketClose) && dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    public void updatePortfolioUnrealizedPnl(double unrealizedPnl) {
        update(ActiveUnderlyingDataField.PORTFOLIO_UNREALIZED_PNL, unrealizedPnl);
    }

    public void resetPortfolioUnrealizedPnl() {
        reset(ActiveUnderlyingDataField.PORTFOLIO_UNREALIZED_PNL);
    }

    public Instrument getCfdInstrument() {
        return cfdInstrument;
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public int getCfdPositionSize() {
        return getCurrent(ActiveUnderlyingDataField.CFD_POSITION_SIZE).intValue();
    }

    public double getCfdUnrealizedPnl() {
        return getCurrent(ActiveUnderlyingDataField.CFD_UNREALIZED_PNL).doubleValue();
    }

    public double getCfdMargin() {
        return getCurrent(ActiveUnderlyingDataField.CFD_MARGIN).doubleValue();
    }

    public int getPutsShort() {
        return getCurrent(ActiveUnderlyingDataField.PUTS_SHORT).intValue();
    }

    public int getPutsLong() {
        return getCurrent(ActiveUnderlyingDataField.PUTS_LONG).intValue();
    }

    public int getCallsShort() {
        return getCurrent(ActiveUnderlyingDataField.CALLS_SHORT).intValue();
    }

    public int getCallsLong() {
        return getCurrent(ActiveUnderlyingDataField.CALLS_LONG).intValue();
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
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_DELTA).doubleValue();
    }

    public double getPortfolioDeltaOnePct() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT).doubleValue();
    }

    public double getPortfolioGamma() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_GAMMA).doubleValue();
    }

    public double getPortfolioGammaOnePctPct() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT).doubleValue();
    }

    public double getPortfolioVega() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_VEGA).doubleValue();
    }

    public double getPortfolioTheta() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_THETA).doubleValue();
    }

    public double getPortfolioTimeValue() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_TIME_VALUE).doubleValue();
    }

    public double getPortfolioUnrealizedPnl() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_UNREALIZED_PNL).doubleValue();
    }

    public double getPortfolioMargin() {
        return getCurrent(ActiveUnderlyingDataField.PORTFOLIO_MARGIN).doubleValue();
    }

    public double getAllocationPct() {
        return getCurrent(ActiveUnderlyingDataField.ALLOCATION_PCT).doubleValue();
    }
}
