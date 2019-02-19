package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 2/18/2019.
 */
public enum RiskResolution {
    EMAIL_ONLY,
    DELTA_HEDGE,
    CLOSE_OFFENDING;

    public String text() {
        return name().replaceAll("_", " ").toLowerCase();
    }
}
