package com.highpowerbear.hpboptions.field;

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
    CFD_UNREALIZED_PNL,
    CFD_MARGIN {
        @Override
        public Number getInitialValue() {
            return 0d;
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
            return thresholdBreachedRange(value);
        }
    },
    PORTFOLIO_GAMMA,
    PORTFOLIO_GAMMA_ONE_PCT_PCT {
        @Override
        public boolean thresholdBreached(Number value) {
            return thresholdBreachedRange(value);
        }
    },
    PORTFOLIO_VEGA,
    PORTFOLIO_THETA,
    PORTFOLIO_TIME_VALUE,
    PORTFOLIO_UNREALIZED_PNL,
    PORTFOLIO_MARGIN,
    ALLOCATION_PCT {
        @Override
        public boolean thresholdBreached(Number value) {
            return thresholdBreachedHigh(value);
        }
    };

    boolean thresholdBreachedRange(Number value) {
        RiskThreshold rt = getRiskThreshold();
        double v = value.doubleValue();

        return v <= rt.getLow().doubleValue() || v >= rt.getHigh().doubleValue();
    }

    boolean thresholdBreachedHigh(Number value) {
        return value.doubleValue() >= getRiskThreshold().getHigh().doubleValue();
    }

    private static List<UnderlyingDataField> fields = Arrays.asList(UnderlyingDataField.values());

    private static List<UnderlyingDataField> cfdFields = Stream.of(
            CFD_POSITION_SIZE,
            CFD_UNREALIZED_PNL,
            CFD_MARGIN
    ).collect(Collectors.toList());

    private static List<UnderlyingDataField> riskDataFields = Stream.of(
            PORTFOLIO_DELTA,
            PORTFOLIO_DELTA_ONE_PCT,
            PORTFOLIO_GAMMA,
            PORTFOLIO_GAMMA_ONE_PCT_PCT,
            PORTFOLIO_VEGA,
            PORTFOLIO_THETA,
            PORTFOLIO_TIME_VALUE,
            PORTFOLIO_MARGIN,
            ALLOCATION_PCT).collect(Collectors.toList());

    public static List<UnderlyingDataField> fields() {
        return fields;
    }

    public static List<UnderlyingDataField> cfdFields() {
        return cfdFields;
    }

    public static List<UnderlyingDataField> riskDataFields() {
        return riskDataFields;
    }}
