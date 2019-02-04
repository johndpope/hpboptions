package com.highpowerbear.hpboptions.enums;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.model.RiskThreshold;

/**
 * Created by robertk on 12/3/2018.
 */
public interface DataField {
    default Number getInitialValue() {
        return Double.NaN;
    }

    default RiskThreshold getRiskThreshold() {
        return HopSettings.getRiskThreshold(this);
    }

    default boolean thresholdBreached(Number value) {
        return false;
    }

    String name();
}
