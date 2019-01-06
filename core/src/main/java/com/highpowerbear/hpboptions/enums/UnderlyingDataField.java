package com.highpowerbear.hpboptions.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/9/2018.
 */
public enum UnderlyingDataField implements DataField {
    IV_CLOSE,
    PORTFOLIO_DELTA,
    PORTFOLIO_GAMMA,
    PORTFOLIO_VEGA,
    PORTFOLIO_THETA,
    PORTFOLIO_TIME_VALUE,
    PORTFOLIO_DELTA_DOLLARS,
    EXPOSURE_PCT,
    UNREALIZED_PNL;

    @Override
    public Number getInitialValue() {
        return Double.NaN;
    }

    private static List<UnderlyingDataField> fields = Arrays.asList(UnderlyingDataField.values());
    private static List<UnderlyingDataField> riskDataFields = Stream.of(
            PORTFOLIO_DELTA,
            PORTFOLIO_GAMMA,
            PORTFOLIO_VEGA,
            PORTFOLIO_THETA,
            PORTFOLIO_TIME_VALUE,
            PORTFOLIO_DELTA_DOLLARS).collect(Collectors.toList());

    public static List<UnderlyingDataField> fields() {
        return fields;
    }

    public static List<UnderlyingDataField> riskDataFields() {
        return riskDataFields;
    }}
