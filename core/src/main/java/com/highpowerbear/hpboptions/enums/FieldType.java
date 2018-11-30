package com.highpowerbear.hpboptions.enums;

import com.ib.client.TickType;
import org.apache.commons.text.CaseUtils;

import java.util.*;

/**
 * Created by robertk on 11/5/2018.
 */
public enum FieldType {
    // basic
    BID (-1.0, true, TickType.BID),
    ASK (-1.0, true, TickType.ASK),
    LAST (-1.0, true, TickType.LAST),
    CLOSE (-1.0, true, TickType.CLOSE),
    BID_SIZE (-1, true, TickType.BID_SIZE),
    ASK_SIZE (-1, true, TickType.ASK_SIZE),
    LAST_SIZE (-1, true, TickType.LAST_SIZE),
    VOLUME (-1, true, TickType.VOLUME),
    OPTION_CALL_VOLUME (-1, false, TickType.OPTION_CALL_VOLUME),
    OPTION_PUT_VOLUME (-1, false, TickType.OPTION_PUT_VOLUME),
    OPTION_CALL_OPEN_INTEREST (-1, false, TickType.OPTION_CALL_OPEN_INTEREST),
    OPTION_PUT_OPEN_INTEREST (-1, false, TickType.OPTION_PUT_OPEN_INTEREST),

    // derived
    CHANGE_PCT (Double.NaN, true, TickType.LAST, TickType.CLOSE),
    OPTION_VOLUME (-1, true, TickType.OPTION_CALL_VOLUME, TickType.OPTION_PUT_VOLUME),
    OPTION_OPEN_INTEREST (-1, true, TickType.OPTION_CALL_OPEN_INTEREST, TickType.OPTION_PUT_OPEN_INTEREST),

    // tbd
    DELTA(Double.NaN, false),
    GAMMA(Double.NaN, false),
    VEGA(Double.NaN, false),
    THETA(Double.NaN, false),
    TIME_VALUE(-1.0, false),
    IMPLIED_VOLATILITY(-1.0, false),
    IMPLIED_VOLATILITY_CHANGE(Double.NaN, false);

    private Number initialValue;
    private boolean createMessage;
    private TickType[] tickTypes;

    FieldType(Number initialValue, boolean createMessage, TickType... tickTypes) {
        this.initialValue = initialValue;
        this.createMessage = createMessage;
        this.tickTypes = tickTypes;
    }

    public boolean isBasic() {
        return tickTypes != null && tickTypes.length == 1;
    }

    public boolean isDerived() {
        return tickTypes != null && tickTypes.length > 1;
    }

    public Number getInitialValue() {
        return initialValue;
    }

    public boolean isCreateMessage() {
        return createMessage;
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
