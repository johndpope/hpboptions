package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.FieldType;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractDataHolder implements DataHolder {

    private final Instrument instrument;
    private final int ibRequestId;
    @JsonIgnore
    private final Map<FieldType, Number> oldValueMap = new HashMap<>();
    @JsonIgnore
    private final Map<FieldType, Number> valueMap = new HashMap<>();

    AbstractDataHolder(Instrument instrument, int ibRequestId) {
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;

        initMap(oldValueMap);
        initMapSpecific(oldValueMap);
        initMap(valueMap);
        initMapSpecific(valueMap);
    }

    private void initMap(Map<FieldType, Number> map) {
        map.put(FieldType.BID, -1.0);
        map.put(FieldType.ASK, -1.0);
        map.put(FieldType.LAST, -1.0);
        map.put(FieldType.CLOSE, -1.0);
        map.put(FieldType.CHANGE_PCT, Double.NaN);

        map.put(FieldType.BID_SIZE, -1);
        map.put(FieldType.ASK_SIZE, -1);
        map.put(FieldType.LAST_SIZE, -1);
        map.put(FieldType.VOLUME, -1);
    }

    protected abstract void initMapSpecific(Map<FieldType, Number> map);

    public abstract String getMessagePrefix();

    @Override
    public void updateField(FieldType fieldType, Number value) {
        update(fieldType, value);
    }

    public void updateField(FieldType fieldType) {
        if (fieldType == FieldType.CHANGE_PCT) {
            double last = valueMap.get(FieldType.LAST).doubleValue();
            double close = valueMap.get(FieldType.CLOSE).doubleValue();

            if (CoreUtil.isValidPrice(last) && CoreUtil.isValidPrice(close)) {
                double changePct = Double.parseDouble(String.format("%.2f", ((last - close) / close) * 100d));
                update(FieldType.CHANGE_PCT, changePct);
            }
        }
    }

    private void update(FieldType fieldType, Number value) {
        oldValueMap.put(fieldType, valueMap.get(fieldType));
        valueMap.put(fieldType, value);
    }

    @Override
    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public int getIbRequestId() {
        return ibRequestId;
    }

    @Override
    public String createMessage(FieldType fieldType) {
        return getMessagePrefix() + "," + ibRequestId + "," + fieldType.toCamelCase() + "," + oldValueMap.get(fieldType) + "," + valueMap.get(fieldType);
    }

    @Override
    public double getBid() {
        return valueMap.get(FieldType.BID).doubleValue();
    }

    @Override
    public double getAsk() {
        return valueMap.get(FieldType.ASK).doubleValue();
    }

    @Override
    public double getLast() {
        return valueMap.get(FieldType.LAST).doubleValue();
    }

    @Override
    public double getClose() {
        return valueMap.get(FieldType.CLOSE).doubleValue();
    }

    @Override
    public double getChangePct() {
        return valueMap.get(FieldType.CHANGE_PCT).doubleValue();
    }

    @Override
    public int getBidSize() {
        return valueMap.get(FieldType.BID_SIZE).intValue();
    }

    @Override
    public int getAskSize() {
        return valueMap.get(FieldType.ASK_SIZE).intValue();
    }

    @Override
    public int getLastSize() {
        return valueMap.get(FieldType.LAST_SIZE).intValue();
    }

    @Override
    public int getVolume() {
        return valueMap.get(FieldType.VOLUME).intValue();
    }
}
