package com.highpowerbear.hpboptions.enums;

import com.ib.client.TickType;
import org.apache.commons.text.CaseUtils;

import java.util.*;

/**
 * Created by robertk on 11/5/2018.
 */
public enum FieldType {
    BID (TickType.BID),
    ASK (TickType.ASK),
    LAST (TickType.LAST),
    CLOSE (TickType.CLOSE),
    BID_SIZE (TickType.BID_SIZE),
    ASK_SIZE (TickType.ASK_SIZE),
    LAST_SIZE (TickType.LAST_SIZE),
    VOLUME (TickType.VOLUME),
    CHANGE_PCT (TickType.LAST, TickType.CLOSE),
    OPTION_VOLUME,
    OPTION_AVG_VOLUME,
    OPTION_OPEN_INTEREST,
    DELTA,
    GAMMA,
    VEGA,
    THETA,
    TIME_VALUE,
    IMPLIED_VOLATILITY,
    IMPLIED_VOLATILITY_CHANGE_;

    private TickType[] tickTypes;

    FieldType(TickType... tickTypes) {
        this.tickTypes = tickTypes;
    }

    public boolean isBasic() {
        return tickTypes != null && tickTypes.length == 1;
    }

    public boolean isDerived() {
        return tickTypes != null && tickTypes.length > 1;
    }

    public String toCamelCase() {
        return CaseUtils.toCamelCase(name(), false, '_');
    }

    private static final Map<TickType, Set<FieldType>> tickFieldMap = new HashMap<>();
    static {
        for (FieldType fieldType : FieldType.values()) {
            for (TickType tickType : fieldType.tickTypes) {
                tickFieldMap.putIfAbsent(tickType, new HashSet<>());
                tickFieldMap.get(tickType).add(fieldType);
            }
        }
    }

    public static Set<FieldType> getFieldTypes(TickType tickType) {
        return tickFieldMap.get(tickType);
    }
}
