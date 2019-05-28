package com.highpowerbear.hpboptions.field;

import java.util.*;

/**
 * Created by robertk on 12/3/2018.
 */
public enum DerivedMarketDataField implements DataField {

    CHANGE (BasicMarketDataField.LAST, BasicMarketDataField.CLOSE),
    CHANGE_PCT (BasicMarketDataField.LAST, BasicMarketDataField.CLOSE),
    IV_CHANGE_PCT (BasicMarketDataField.OPTION_IMPLIED_VOL),
    IV_RANK (BasicMarketDataField.OPTION_IMPLIED_VOL),
    OPTION_VOLUME (BasicMarketDataField.OPTION_CALL_VOLUME, BasicMarketDataField.OPTION_PUT_VOLUME) {
        @Override
        public Number getInitialValue() {
            return -1;
        }
    },
    OPTION_OPEN_INTEREST (BasicMarketDataField.OPTION_CALL_OPEN_INTEREST, BasicMarketDataField.OPTION_PUT_OPEN_INTEREST) {
        @Override
        public Number getInitialValue() {
            return -1;
        }
    };

    private Set<BasicMarketDataField> dependencies;

    DerivedMarketDataField(BasicMarketDataField... dependencies) {
        this.dependencies = new HashSet<>(Arrays.asList(dependencies));
    }

    public Set<BasicMarketDataField> getDependencies() {
        return dependencies;
    }

    private static Map<BasicMarketDataField, Set<DerivedMarketDataField>> basicDerivedMap = new HashMap<>();
    static {
        Arrays.stream(BasicMarketDataField.values()).forEach(field -> basicDerivedMap.put(field, new HashSet<>()));
        for (DerivedMarketDataField derivedField : DerivedMarketDataField.values()) {
            for (BasicMarketDataField basicField : derivedField.dependencies) {
                basicDerivedMap.get(basicField).add(derivedField);
            }
        }
    }

    public static Set<DerivedMarketDataField> derivedFields(BasicMarketDataField basicField) {
        return basicDerivedMap.get(basicField);
    }

    private static List<DerivedMarketDataField> fields = Arrays.asList(DerivedMarketDataField.values());

    public static List<DerivedMarketDataField> fields() {
        return fields;
    }
}
