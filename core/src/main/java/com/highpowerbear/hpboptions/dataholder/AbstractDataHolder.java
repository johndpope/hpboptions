package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.DataField;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 6/6/2019.
 */
public abstract class AbstractDataHolder implements DataHolder {

    protected String id;
    protected final DataHolderType type;

    protected final Map<DataField, CircularFifoQueue<Number>> valueMap = new HashMap<>(); // field -> queue[value, oldValue]

    public AbstractDataHolder(DataHolderType type) {
        this.type = type;
    }

    protected void update(DataField field, Number value) {
        valueMap.get(field).add(value instanceof Double ? HopUtil.round4(value.doubleValue()) : value);
    }

    protected void reset(DataField field) {
        update(field, field.getInitialValue());
    }

    protected CircularFifoQueue<Number> createValueQueue(Number initialValue) {
        CircularFifoQueue<Number> valueQueue = new CircularFifoQueue<>(2);
        valueQueue.add(initialValue); // old value
        valueQueue.add(initialValue); // current value

        return valueQueue;
    }

    public Number getCurrent(DataField field) {
        return valueMap.get(field).get(1);
    }

    protected Number getOld(DataField field) {
        return valueMap.get(field).peek();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DataHolderType getType() {
        return type;
    }

    @Override
    public String createMessage(DataField dataField) {
        return id + "," + HopUtil.toCamelCase(dataField.name()) + "," + getOld(dataField) + "," + getCurrent(dataField);
    }

    @Override
    public boolean isSendMessage(DataField field) {
        return true;
    }
}
