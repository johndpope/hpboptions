package com.highpowerbear.hpboptions.enums;

import com.highpowerbear.hpboptions.model.RiskThreshold;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/9/2018.
 */
public enum UnderlyingDataField implements DataField {
    CFD_POSITION_SIZE {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    IV_CLOSE,
    PUTS_SUM {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    CALLS_SUM {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    PORTFOLIO_DELTA,
    PORTFOLIO_DELTA_ONE_PCT {
        @Override
        public boolean thresholdBreached(Number value) {
            RiskThreshold rt = getRiskThreshold();
            double v = value.doubleValue();

            return v <= rt.getLow().doubleValue() || v >= rt.getHigh().doubleValue();
        }
    },
    PORTFOLIO_GAMMA,
    PORTFOLIO_GAMMA_ONE_PCT_PCT,
    PORTFOLIO_VEGA,
    PORTFOLIO_THETA,
    PORTFOLIO_TIME_VALUE,
    ALLOCATION_PCT {
        @Override
        public boolean thresholdBreached(Number value) {
            return value.doubleValue() >= getRiskThreshold().getHigh().doubleValue();
        }
    },
    UNREALIZED_PNL;

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
