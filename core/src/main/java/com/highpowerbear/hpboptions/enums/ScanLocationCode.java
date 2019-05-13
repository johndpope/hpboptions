package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 5/13/2019.
 */
public enum ScanLocationCode {
    STK_US_MAJOR,
    ETF_EQ_US_MAJOR;

    public String getCode() {
        return name().replace("_", ".");
    }
}
