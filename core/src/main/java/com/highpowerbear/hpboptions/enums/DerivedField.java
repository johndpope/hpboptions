package com.highpowerbear.hpboptions.enums;

import java.util.*;

/**
 * Created by robertk on 12/3/2018.
 */
public enum DerivedField implements Field  {
    CHANGE_PCT (Double.NaN, BasicField.LAST, BasicField.CLOSE),
    OPTION_VOLUME (-1, BasicField.OPTION_CALL_VOLUME, BasicField.OPTION_PUT_VOLUME),
    OPTION_OPEN_INTEREST (-1, BasicField.OPTION_CALL_OPEN_INTEREST, BasicField.OPTION_PUT_OPEN_INTEREST);

    private Number initialValue;
    private Set<BasicField> dependencies;

    DerivedField(Number initialValue, BasicField... dependencies) {
        this.initialValue = initialValue;
        this.dependencies = new HashSet<>(Arrays.asList(dependencies));
    }

    @Override
    public Number getInitialValue() {
        return initialValue;
    }

    public Set<BasicField> getDependencies() {
        return dependencies;
    }

    private static Map<BasicField, Set<DerivedField>> basicDerivedMap = new HashMap<>();
    static {
        Arrays.stream(BasicField.values()).forEach(field -> basicDerivedMap.put(field, new HashSet<>()));
        for (DerivedField derivedField : DerivedField.values()) {
            for (BasicField basicField : derivedField.dependencies) {
                basicDerivedMap.get(basicField).add(derivedField);
            }
        }
    }

    public static Set<DerivedField> getDerivedFields(BasicField basicField) {
        return basicDerivedMap.get(basicField);
    }
}
