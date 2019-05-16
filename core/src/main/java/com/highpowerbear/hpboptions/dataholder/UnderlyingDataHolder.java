package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.field.DataField;

import java.time.LocalDate;
import java.util.Set;

/**
 * Created by robertk on 5/16/2019.
 */
public interface UnderlyingDataHolder extends MarketDataHolder {
    void addImpliedVolatility(LocalDate date, double impliedVolatility);
    void impliedVolatilityHistoryCompleted();
    Set<DataField> getIvHistoryDependentFields();
    int getIbHistDataRequestId();
}
