package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 5/13/2019.
 */
public enum ScanInstrumentCode {
    STK,        // US Stocks
    ETF_EQ_US;  // US Equity ETFs

    public String getCode() {
        return name().replace("_", ".");
    }
}
