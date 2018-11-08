package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 10/30/2018.
 */
public enum Exchange {
    SMART,
    IDEALPRO,
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
