package com.highpowerbear.hpboptions.field;

import com.highpowerbear.hpboptions.model.RiskThreshold;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 12/9/2018.
 */
public enum ActiveUnderlyingDataField implements DataField {
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
    PUTS_SHORT {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    PUTS_LONG {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    CALLS_SHORT {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    CALLS_LONG {
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

    private static List<ActiveUnderlyingDataField> fields = Arrays.asList(ActiveUnderlyingDataField.values());

    private static List<ActiveUnderlyingDataField> cfdFields = Stream.of(
            CFD_POSITION_SIZE,
            CFD_UNREALIZED_PNL,
            CFD_MARGIN
    ).collect(Collectors.toList());

    private static List<ActiveUnderlyingDataField> optionPositionSumFields = Stream.of(
            PUTS_SHORT,
            PUTS_LONG,
            CALLS_SHORT,
            CALLS_LONG
    ).collect(Collectors.toList());

    private static List<ActiveUnderlyingDataField> riskDataFields = Stream.of(
            PORTFOLIO_DELTA,
            PORTFOLIO_DELTA_ONE_PCT,
            PORTFOLIO_GAMMA,
            PORTFOLIO_GAMMA_ONE_PCT_PCT,
            PORTFOLIO_VEGA,
            PORTFOLIO_THETA,
            PORTFOLIO_TIME_VALUE,
            PORTFOLIO_MARGIN,
            ALLOCATION_PCT).collect(Collectors.toList());

    public static List<ActiveUnderlyingDataField> fields() {
        return fields;
    }

    public static List<ActiveUnderlyingDataField> cfdFields() {
        return cfdFields;
    }

    public static List<ActiveUnderlyingDataField> optionPositionSumFields() {
        return optionPositionSumFields;
    }

    public static List<ActiveUnderlyingDataField> riskDataFields() {
        return riskDataFields;
    }}
