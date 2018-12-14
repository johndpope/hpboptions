package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.*;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractDataHolder implements DataHolder {

    protected final String id;
    private final DataHolderType type;
    private final Instrument instrument;
    private final int ibMktDataRequestId;
    private final Set<DataField> fieldsToDisplay = new HashSet<>();
    private String genericTicks;

    protected final Map<DataField, CircularFifoQueue<Number>> valueMap = new HashMap<>(); // field -> queue[value, oldValue]

    AbstractDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId) {
        this.type = type;
        this.instrument = instrument;
        this.ibMktDataRequestId = ibMktDataRequestId;
        id = type.name().toLowerCase() + "-" + instrument.getId();

        BasicMktDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
        DerivedMktDataField.getValues().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        Stream.of(
                BasicMktDataField.BID,
                BasicMktDataField.ASK,
                BasicMktDataField.LAST,
                BasicMktDataField.CLOSE,
                BasicMktDataField.BID_SIZE,
                BasicMktDataField.ASK_SIZE,
                BasicMktDataField.LAST_SIZE,
                BasicMktDataField.VOLUME,
                DerivedMktDataField.CHANGE_PCT
        ).forEach(fieldsToDisplay::add);

        determineGenericTicks();
    }

    protected void addFieldsToDisplay(Set<DataField> fieldsToDisplay) {
        this.fieldsToDisplay.addAll(fieldsToDisplay);
        determineGenericTicks();
    }

    private void determineGenericTicks() {
        Set<Integer> genericTicksSet = new HashSet<>();

        Arrays.stream(BasicMktDataField.values())
                .filter(fieldsToDisplay::contains)
                .map(BasicMktDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        Arrays.stream(DerivedMktDataField.values())
                .filter(fieldsToDisplay::contains)
                .flatMap(derivedField -> derivedField.getDependencies().stream())
                .map(BasicMktDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        genericTicks = StringUtils.join(genericTicksSet, ",");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void updateField(BasicMktDataField field, Number value) {
        update(field, convert(field, value));
    }

    @Override
    public void calculateField(DerivedMktDataField field) {
        if (field == DerivedMktDataField.CHANGE_PCT) {
            double l = getCurrent(BasicMktDataField.LAST).doubleValue();
            double c = getCurrent(BasicMktDataField.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                double value = ((l - c) / c) * 100d;
                update(field, CoreUtil.round2(value));
            }

        } else if (field == DerivedMktDataField.OPTION_OPEN_INTEREST) {
            int o = getCurrent(BasicMktDataField.OPTION_CALL_OPEN_INTEREST).intValue();
            int p = getCurrent(BasicMktDataField.OPTION_PUT_OPEN_INTEREST).intValue();

            int value = (isValidSize(o) ? o : 0) + (isValidSize(p) ? p : 0);
            update(field, value);
        }
    }

    private Number convert(BasicMktDataField field, Number value) {
        if (type == DataHolderType.UNDERLYING && field == BasicMktDataField.VOLUME && isValidSize(value.intValue())) {
            return value.intValue() * 100;
        }

        return value;
    }

    protected void update(DataField field, Number value) {
        valueMap.get(field).add(value);
    }

    protected CircularFifoQueue<Number> createValueQueue(Number initialValue) {
        CircularFifoQueue<Number> valueQueue = new CircularFifoQueue<>(2);
        valueQueue.add(initialValue); // old value
        valueQueue.add(initialValue); // current value

        return valueQueue;
    }

    protected Number getCurrent(DataField field) {
        return valueMap.get(field).get(1);
    }

    protected Number getOld(DataField field) {
        return valueMap.get(field).peek();
    }

    protected boolean isValidPrice(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && d > 0d && d != Double.MAX_VALUE;
    }

    protected boolean isValidSize(int i) {
        return i > 0 && i != Integer.MAX_VALUE;
    }

    @Override
    public DataHolderType getType() {
        return type;
    }

    @Override
    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public int getIbMktDataRequestId() {
        return ibMktDataRequestId;
    }

    @Override
    public String createMessage(DataField dataField) {
        return id + "," + CoreUtil.toCamelCase(dataField.name()) + "," + getOld(dataField) + "," + getCurrent(dataField);
    }

    @Override
    public boolean isSendMessage(DataField field) {
        return fieldsToDisplay.contains(field) &&
                (Math.abs(getCurrent(field).doubleValue() - getOld(field).doubleValue()) > CoreSettings.DATA_FIELD_MIN_CHANGE_TO_SEND);
    }

    @Override
    public String getGenericTicks() {
        return genericTicks;
    }

    public double getBid() {
        return getCurrent(BasicMktDataField.BID).doubleValue();
    }

    public double getAsk() {
        return getCurrent(BasicMktDataField.ASK).doubleValue();
    }

    public double getLast() {
        return getCurrent(BasicMktDataField.LAST).doubleValue();
    }

    public double getClose() {
        return getCurrent(BasicMktDataField.CLOSE).doubleValue();
    }

    public int getBidSize() {
        return getCurrent(BasicMktDataField.BID_SIZE).intValue();
    }

    public int getAskSize() {
        return getCurrent(BasicMktDataField.ASK_SIZE).intValue();
    }

    public int getLastSize() {
        return getCurrent(BasicMktDataField.LAST_SIZE).intValue();
    }

    public int getVolume() {
        return getCurrent(BasicMktDataField.VOLUME).intValue();
    }

    public double getChangePct() {
        return getCurrent(DerivedMktDataField.CHANGE_PCT).doubleValue();
    }
}
