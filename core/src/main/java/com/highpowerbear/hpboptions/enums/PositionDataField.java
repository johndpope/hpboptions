package com.highpowerbear.hpboptions.enums;

import com.highpowerbear.hpboptions.common.HopSettings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robertk on 12/10/2018.
 */
public enum PositionDataField implements DataField {

    POSITION_SIZE {
        @Override
        public Number getInitialValue() {
            return HopSettings.INVALID_POSITION;
        }
    },
    UNREALIZED_PNL,
    MARGIN;

    private static List<PositionDataField> fields = Arrays.asList(PositionDataField.values());
    public static List<PositionDataField> fields() {
        return fields;
    }
}
