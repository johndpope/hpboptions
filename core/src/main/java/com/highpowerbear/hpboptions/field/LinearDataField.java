package com.highpowerbear.hpboptions.field;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robertk on 5/28/2019.
 */
public enum LinearDataField implements DataField {
    POSITION_SIZE {
        @Override
        public Number getInitialValue() {
            return 0;
        }
    },
    UNREALIZED_PNL;

    private static List<PositionDataField> fields = Arrays.asList(PositionDataField.values());

    public static List<PositionDataField> fields() {
        return fields;
    }
}
