package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 12/8/2018.
 */
public enum IbTradingHours {
    REGULAR(1),
    EXTENDED(0);

    private int value;

    IbTradingHours(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
