package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 12/3/2018.
 */
public interface DataField {
    default Number getInitialValue() {
        return Double.NaN;
    }
    String name();
}
