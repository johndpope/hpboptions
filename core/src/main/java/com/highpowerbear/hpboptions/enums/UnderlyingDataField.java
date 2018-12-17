package com.highpowerbear.hpboptions.enums;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robertk on 12/9/2018.
 */
public enum UnderlyingDataField implements DataField {
    IV_CLOSE,
    DELTA_CUMULATIVE,
    GAMMA_CUMULATIVE,
    VEGA_CUMULATIVE,
    THETA_CUMULATIVE,
    DELTA_DOLLARS_CUMULATIVE,
    TIME_VALUE_CUMULATIVE,
    UNREALIZED_PNL_CUMULATIVE;

    @Override
    public Number getInitialValue() {
        return Double.NaN;
    }

    private static List<UnderlyingDataField> values = Arrays.asList(UnderlyingDataField.values());
    public static List<UnderlyingDataField> getValues() {
        return values;
    }
}
