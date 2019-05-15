package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 5/13/2019.
 */
public enum ScanInstrumentCode {
    STK("US Stocks", ScanLocationCode.STK_US_MAJOR),
    ETF_EQ_US("USD Equity ETFs", ScanLocationCode.ETF_EQ_US_MAJOR);

    private String name;
    private ScanLocationCode locationCode;

    ScanInstrumentCode(String name, ScanLocationCode locationCode) {
        this.name = name;
        this.locationCode = locationCode;
    }

    public String getName() {
        return name;
    }

    public ScanLocationCode getLocationCode() {
        return locationCode;
    }

    public String getCode() {
        return name().replace("_", ".");
    }
}
