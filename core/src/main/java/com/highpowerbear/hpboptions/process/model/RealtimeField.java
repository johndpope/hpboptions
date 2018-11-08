package com.highpowerbear.hpboptions.process.model;

import com.highpowerbear.hpboptions.enums.RealtimeFieldType;
import com.highpowerbear.hpboptions.enums.RealtimeStatus;

/**
 * Created by robertk on 11/5/2018.
 */
public class RealtimeField<T> {
    private T value;
    private RealtimeStatus status;
    private RealtimeFieldType fieldType;

    RealtimeField(T value, RealtimeStatus status, RealtimeFieldType fieldType) {
        this.value = value;
        this.status = status;
        this.fieldType = fieldType;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public RealtimeStatus getStatus() {
        return status;
    }

    public void setStatus(RealtimeStatus status) {
        this.status = status;
    }

    public RealtimeFieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(RealtimeFieldType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
