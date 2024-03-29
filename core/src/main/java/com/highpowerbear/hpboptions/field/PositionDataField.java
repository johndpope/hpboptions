package com.highpowerbear.hpboptions.field;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robertk on 12/10/2018.
 */
public enum PositionDataField implements DataField {

    POSITION_SIZE {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    UNREALIZED_PNL,
    MARGIN;

    private static List<PositionDataField> fields = Arrays.asList(PositionDataField.values());

    public static List<PositionDataField> fields() {
        return fields;
    }
}
