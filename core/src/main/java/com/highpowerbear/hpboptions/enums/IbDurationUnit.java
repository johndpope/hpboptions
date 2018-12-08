package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 12/8/2018.
 */
public enum IbDurationUnit {
    SECOND_1("1 S"),
    DAY_1("1 D"),
    WEEK_1("1 W"),
    MONTH_1("1 M"),
    YEAR_1("1 Y");

    private String value;

    IbDurationUnit(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
