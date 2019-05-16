package com.highpowerbear.hpboptions.field;

import java.util.Arrays;
import java.util.List;

/**
 * Created by robertk on 5/16/2019.
 */
public enum UnderlyingDataField implements DataField {
    IV_CLOSE;

    private static List<UnderlyingDataField> fields = Arrays.asList(UnderlyingDataField.values());

    public static List<UnderlyingDataField> fields() {
        return fields;
    }
}
