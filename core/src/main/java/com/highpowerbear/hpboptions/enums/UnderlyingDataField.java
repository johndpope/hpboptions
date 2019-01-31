package com.highpowerbear.hpboptions.enums;

import com.highpowerbear.hpboptions.common.HopSettings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/9/2018.
 */
public enum UnderlyingDataField implements DataField {
    IV_CLOSE,
    PUTS_SUM {
        @Override
        public Number getInitialValue() {
            return HopSettings.INVALID_POSITION;
        }
    },
    CALLS_SUM {
        @Override
        public Number getInitialValue() {
            return HopSettings.INVALID_POSITION;
        }
    },
    PORTFOLIO_DELTA,
    PORTFOLIO_DELTA_ONE_PCT {
        @Override
        public boolean thresholdBreached(double value) {
            return Math.abs(value) >= HopSettings.getRiskThreshold(this);
        }
    },
    PORTFOLIO_GAMMA,
    PORTFOLIO_GAMMA_ONE_PCT_PCT,
    PORTFOLIO_VEGA,
    PORTFOLIO_THETA,
    PORTFOLIO_TIME_VALUE,
    ALLOCATION_PCT {
        @Override
        public boolean thresholdBreached(double value) {
            return value >= HopSettings.getRiskThreshold(this);
        }
    },
    UNREALIZED_PNL;

    public boolean thresholdBreached(double value) {
        return false;
    }

    private static List<UnderlyingDataField> fields = Arrays.asList(UnderlyingDataField.values());
    private static List<UnderlyingDataField> riskDataFields = Stream.of(
            PORTFOLIO_DELTA,
            PORTFOLIO_DELTA_ONE_PCT,
            PORTFOLIO_GAMMA,
            PORTFOLIO_GAMMA_ONE_PCT_PCT,
            PORTFOLIO_VEGA,
            PORTFOLIO_THETA,
            PORTFOLIO_TIME_VALUE,
            ALLOCATION_PCT).collect(Collectors.toList());

    public static List<UnderlyingDataField> fields() {
        return fields;
    }

    public static List<UnderlyingDataField> riskDataFields() {
        return riskDataFields;
    }}
