package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 12/8/2018.
 */
public enum IbBarSize {
    SECOND_1("1 secs"),
    SECOND_5("5 secs"),
    SECOND_10("10 secs"),
    SECOND_15("15 secs"),
    SECOND_30("30 secs"),
    MIN_1("1 min"),
    MIN_2("2 mins"),
    MIN_3("3 mins"),
    MIN_5("5 mins"),
    MIN_10("10 mins"),
    MIN_15("15 mins"),
    MIN_20("20 mins"),
    MIN_30("30 mins"),
    HOUR_1("1 hour"),
    HOUR_2("2 hours"),
    HOUR_3("3 hours"),
    HOUR_4("4 hours"),
    HOUR_8("8 hours"),
    DAY_1("1 day"),
    WEEK_1("1 week"),
    MONTH_1("1 month"),;

    private String value;

    IbBarSize(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}


