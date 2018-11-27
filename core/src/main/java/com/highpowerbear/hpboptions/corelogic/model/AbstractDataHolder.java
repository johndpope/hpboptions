package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractDataHolder implements DataHolder {

    private final Instrument instrument;
    final int ibRequestId;
    @JsonIgnore
    final Map<FieldType, Number> valueMap = new HashMap<>();

    AbstractDataHolder(Instrument instrument, int ibRequestId) {
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;

        valueMap.put(FieldType.BID, Double.NaN);
        valueMap.put(FieldType.ASK, Double.NaN);
        valueMap.put(FieldType.LAST, Double.NaN);
        valueMap.put(FieldType.CLOSE, Double.NaN);
        valueMap.put(FieldType.CHANGE_PCT, Double.NaN);

        valueMap.put(FieldType.BID_SIZE, -1);
        valueMap.put(FieldType.ASK_SIZE, -1);
        valueMap.put(FieldType.LAST_SIZE, -1);
        valueMap.put(FieldType.VOLUME, -1);
    }

    @Override
    public void updateValue(FieldType fieldType, Number value) {
        valueMap.put(fieldType, value);

        if (fieldType == FieldType.LAST) {
            updateChangePct();
        }
    }

    private void updateChangePct() {
        Double last = valueMap.get(FieldType.LAST).doubleValue();
        Double close = valueMap.get(FieldType.CLOSE).doubleValue();

        if (!last.isNaN() && !close.isNaN()) {
            valueMap.put(FieldType.CHANGE_PCT, ((last - close) / close) * 100d);
        }
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
    public abstract String createMessage(FieldType fieldType);

    @Override
    public abstract String getWsTopic();

    public double getBid() {
        return valueMap.get(FieldType.BID).doubleValue();
    }

    public double getAsk() {
        return valueMap.get(FieldType.ASK).doubleValue();
    }

    public double getLast() {
        return valueMap.get(FieldType.LAST).doubleValue();
    }

    public double getClose() {
        return valueMap.get(FieldType.CLOSE).doubleValue();
    }

    public double getChangePct() {
        return valueMap.get(FieldType.CHANGE_PCT).doubleValue();
    }

    public int getBidSize() {
        return valueMap.get(FieldType.BID_SIZE).intValue();
    }

    public int getAskSize() {
        return valueMap.get(FieldType.ASK_SIZE).intValue();
    }

    public int getLastSize() {
        return valueMap.get(FieldType.LAST_SIZE).intValue();
    }

    public int geVolume() {
        return valueMap.get(FieldType.VOLUME).intValue();
    }
}
