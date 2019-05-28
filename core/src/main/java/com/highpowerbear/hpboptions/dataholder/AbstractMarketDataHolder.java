package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.field.BasicMarketDataField;
import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.DerivedMarketDataField;
import com.highpowerbear.hpboptions.model.Instrument;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractMarketDataHolder implements MarketDataHolder {

    protected String id;
    private final DataHolderType type;
    protected final Instrument instrument;
    private final int ibMktDataRequestId;

    private final Set<DataField> fieldsToDisplay = new HashSet<>();
    private String genericTicks;

    private int displayRank;

    protected final Map<DataField, CircularFifoQueue<Number>> valueMap = new HashMap<>(); // field -> queue[value, oldValue]

    AbstractMarketDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId) {
        this.type = type;
        this.instrument = instrument;
        this.ibMktDataRequestId = ibMktDataRequestId;

        id = type.name().toLowerCase() + "-" + instrument.getConid();

        BasicMarketDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
        DerivedMarketDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        Stream.of(
                BasicMarketDataField.BID,
                BasicMarketDataField.ASK,
                BasicMarketDataField.LAST,
                BasicMarketDataField.CLOSE,
                BasicMarketDataField.BID_SIZE,
                BasicMarketDataField.ASK_SIZE,
                BasicMarketDataField.LAST_SIZE,
                BasicMarketDataField.VOLUME
        ).forEach(fieldsToDisplay::add);

        determineGenericTicks();
    }

    protected void addFieldsToDisplay(Set<DataField> fieldsToDisplay) {
        this.fieldsToDisplay.addAll(fieldsToDisplay);
        determineGenericTicks();
    }

    private void determineGenericTicks() {
        Set<Integer> genericTicksSet = new HashSet<>();

        BasicMarketDataField.fields().stream()
                .filter(fieldsToDisplay::contains)
                .map(BasicMarketDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        DerivedMarketDataField.fields().stream()
                .filter(fieldsToDisplay::contains)
                .flatMap(derivedField -> derivedField.getDependencies().stream())
                .map(BasicMarketDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        genericTicks = StringUtils.join(genericTicksSet, ",");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void updateField(BasicMarketDataField field, Number value) {
        update(field, convert(field, value));
    }

    @Override
    public void calculateField(DerivedMarketDataField field) {
        if (field == DerivedMarketDataField.CHANGE) {
            double l = getCurrent(BasicMarketDataField.LAST).doubleValue();
            double c = getCurrent(BasicMarketDataField.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                double value = l - c;
                update(field, value);
            }

        } else if (field == DerivedMarketDataField.CHANGE_PCT) {
            double l = getCurrent(BasicMarketDataField.LAST).doubleValue();
            double c = getCurrent(BasicMarketDataField.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                double value = ((l - c) / c) * 100d;
                update(field, value);
            }

        } else if (field == DerivedMarketDataField.OPTION_OPEN_INTEREST) {
            int o = getCurrent(BasicMarketDataField.OPTION_CALL_OPEN_INTEREST).intValue();
            int p = getCurrent(BasicMarketDataField.OPTION_PUT_OPEN_INTEREST).intValue();

            int value = (isValidSize(o) ? o : 0) + (isValidSize(p) ? p : 0);
            update(field, value);
        }
    }

    private Number convert(BasicMarketDataField field, Number value) {
        if (type == DataHolderType.UNDERLYING && field == BasicMarketDataField.VOLUME && isValidSize(value.intValue())) {
            return value.intValue() * 100;
        }

        return value;
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

    protected boolean isValidPrice(double d) {
        return HopUtil.isValidPrice(d);
    }

    protected boolean isValidSize(int i) {
        return HopUtil.isValidSize(i);
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
    public int getDisplayRank() {
        return displayRank;
    }

    public void setDisplayRank(int displayRank) {
        this.displayRank = displayRank;
    }

    @Override
    public String createMessage(DataField dataField) {
        return id + "," + HopUtil.toCamelCase(dataField.name()) + "," + getOld(dataField) + "," + getCurrent(dataField);
    }

    @Override
    public boolean isSendMessage(DataField field) {
        return fieldsToDisplay.contains(field);
    }

    @Override
    public String getGenericTicks() {
        return genericTicks;
    }

    public double getBid() {
        return getCurrent(BasicMarketDataField.BID).doubleValue();
    }

    public double getAsk() {
        return getCurrent(BasicMarketDataField.ASK).doubleValue();
    }

    public double getLast() {
        return getCurrent(BasicMarketDataField.LAST).doubleValue();
    }

    public double getClose() {
        return getCurrent(BasicMarketDataField.CLOSE).doubleValue();
    }

    public int getBidSize() {
        return getCurrent(BasicMarketDataField.BID_SIZE).intValue();
    }

    public int getAskSize() {
        return getCurrent(BasicMarketDataField.ASK_SIZE).intValue();
    }

    public int getLastSize() {
        return getCurrent(BasicMarketDataField.LAST_SIZE).intValue();
    }

    public int getVolume() {
        return getCurrent(BasicMarketDataField.VOLUME).intValue();
    }

    public double getChange() {
        return getCurrent(DerivedMarketDataField.CHANGE).doubleValue();
    }

    public double getChangePct() {
        return getCurrent(DerivedMarketDataField.CHANGE_PCT).doubleValue();
    }
}
