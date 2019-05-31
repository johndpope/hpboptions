package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 10/30/2018.
 */
public enum Exchange {
    SMART,
    ARCA,
    NASDAQ,
    NYSE,
    IDEALPRO,
    IBCFD,
    CBOE,
    GLOBEX,
    ECBOT,
    NYMEX,
    DTB,
    OSE_JPN,
    HKFE,
    KSE;

    public String getCode() {
        return name().replace("_", ".");
    }
}
