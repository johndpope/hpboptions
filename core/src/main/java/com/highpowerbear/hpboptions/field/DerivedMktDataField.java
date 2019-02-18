package com.highpowerbear.hpboptions.field;

import java.util.*;

/**
 * Created by robertk on 12/3/2018.
 */
public enum DerivedMktDataField implements DataField {

    CHANGE (BasicMktDataField.LAST, BasicMktDataField.CLOSE),
    CHANGE_PCT (BasicMktDataField.LAST, BasicMktDataField.CLOSE),
    IV_CHANGE_PCT (BasicMktDataField.OPTION_IMPLIED_VOL),
    IV_RANK (BasicMktDataField.OPTION_IMPLIED_VOL),
    OPTION_VOLUME (BasicMktDataField.OPTION_CALL_VOLUME, BasicMktDataField.OPTION_PUT_VOLUME) {
        @Override
        public Number getInitialValue() {
            return -1;
        }
    },
    OPTION_OPEN_INTEREST (BasicMktDataField.OPTION_CALL_OPEN_INTEREST, BasicMktDataField.OPTION_PUT_OPEN_INTEREST) {
        @Override
        public Number getInitialValue() {
            return -1;
        }
    };

    private Set<BasicMktDataField> dependencies;

    DerivedMktDataField(BasicMktDataField... dependencies) {
        this.dependencies = new HashSet<>(Arrays.asList(dependencies));
    }

    public Set<BasicMktDataField> getDependencies() {
        return dependencies;
    }

    private static Map<BasicMktDataField, Set<DerivedMktDataField>> basicDerivedMap = new HashMap<>();
    static {
        Arrays.stream(BasicMktDataField.values()).forEach(field -> basicDerivedMap.put(field, new HashSet<>()));
        for (DerivedMktDataField derivedField : DerivedMktDataField.values()) {
            for (BasicMktDataField basicField : derivedField.dependencies) {
                basicDerivedMap.get(basicField).add(derivedField);
            }
        }
    }

    public static Set<DerivedMktDataField> derivedFields(BasicMktDataField basicField) {
        return basicDerivedMap.get(basicField);
    }

    private static List<DerivedMktDataField> fields = Arrays.asList(DerivedMktDataField.values());

    public static List<DerivedMktDataField> fields() {
        return fields;
    }
}
