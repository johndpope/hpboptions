package com.highpowerbear.hpboptions.enums;

import java.util.*;

/**
 * Created by robertk on 12/3/2018.
 */
public enum DerivedMktDataField implements MktDataField {
    CHANGE_PCT (Double.NaN, BasicMktDataField.LAST, BasicMktDataField.CLOSE),
    OPTION_VOLUME (-1, BasicMktDataField.OPTION_CALL_VOLUME, BasicMktDataField.OPTION_PUT_VOLUME),
    OPTION_OPEN_INTEREST (-1, BasicMktDataField.OPTION_CALL_OPEN_INTEREST, BasicMktDataField.OPTION_PUT_OPEN_INTEREST);

    private Number initialValue;
    private Set<BasicMktDataField> dependencies;

    DerivedMktDataField(Number initialValue, BasicMktDataField... dependencies) {
        this.initialValue = initialValue;
        this.dependencies = new HashSet<>(Arrays.asList(dependencies));
    }

    @Override
    public Number getInitialValue() {
        return initialValue;
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

    public static Set<DerivedMktDataField> getDerivedFields(BasicMktDataField basicField) {
        return basicDerivedMap.get(basicField);
    }
}
