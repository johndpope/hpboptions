package com.highpowerbear.hpboptions.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/9/2018.
 */
public enum OptionDataField implements DataField {
    DELTA,
    GAMMA,
    VEGA,
    THETA,
    IMPLIED_VOL,
    TIME_VALUE,
    TIME_VALUE_PCT,
    OPTION_PRICE,
    UNDERLYING_PRICE;


    @Override
    public Number getInitialValue() {
        return Double.NaN;
    }

    private static List<OptionDataField> fields = Arrays.asList(OptionDataField.values());
    private static List<OptionDataField> portfolioSourceFields = Stream.of(
            DELTA,
            GAMMA,
            VEGA,
            THETA,
            TIME_VALUE).collect(Collectors.toList());

    public static List<OptionDataField> fields() {
        return fields;
    }

    public static List<OptionDataField> portfolioSourceFields() {
        return portfolioSourceFields;
    }}
