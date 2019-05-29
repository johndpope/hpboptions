package com.highpowerbear.hpboptions.database;

import java.util.List;

/**
 * Created by robertk on 5/29/2019.
 */
public interface HopDao {
    List<Underlying> getActiveUnderlyings();
    List<LinearInstrument> getActiveLinearInstruments();
    void createRiskEvent(RiskEvent riskEvent);
}
