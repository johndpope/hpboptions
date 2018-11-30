package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.FieldType;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 11/27/2018.
 */
public class DataHolder {

    private final DataHolderType type;
    private final Instrument instrument;
    private final int ibRequestId;
    @JsonIgnore
    private final Map<FieldType, Number> oldValueMap = new HashMap<>();
    @JsonIgnore
    private final Map<FieldType, Number> valueMap = new HashMap<>();

    public DataHolder(DataHolderType type, Instrument instrument, int ibRequestId) {
        this.type = type;
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;

        for (FieldType fieldType : FieldType.values()) {
            oldValueMap.put(fieldType, fieldType.getInitialValue());
            valueMap.put(fieldType, fieldType.getInitialValue());
        }
    }

    public void updateField(FieldType fieldType, Number value) {
        update(fieldType, value);
    }

    public void calculateField(FieldType fieldType) {
        if (fieldType == FieldType.CHANGE_PCT) {
            double l = valueMap.get(FieldType.LAST).doubleValue();
            double c = valueMap.get(FieldType.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                update(fieldType, Double.parseDouble(String.format("%.2f", ((l - c) / c) * 100d)));
            }

        } else if (fieldType == FieldType.OPTION_VOLUME) {
            int o = valueMap.get(FieldType.OPTION_CALL_VOLUME).intValue();
            int p = valueMap.get(FieldType.OPTION_PUT_VOLUME).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(fieldType, o + p);
            }

        } else if (fieldType == FieldType.OPTION_OPEN_INTEREST) {
            int o = valueMap.get(FieldType.OPTION_CALL_OPEN_INTEREST).intValue();
            int p = valueMap.get(FieldType.OPTION_PUT_OPEN_INTEREST).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(fieldType, o + p);
            }
        }
    }

    private void update(FieldType fieldType, Number value) {
        if (valueMap.get(fieldType) != null) {
            oldValueMap.put(fieldType, valueMap.get(fieldType));
            valueMap.put(fieldType, value);
        }
    }

    private boolean isValidPrice(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && d > 0d;
    }

    private boolean isValidSize(int i) {
        return i > 0;
    }

    public DataHolderType getType() {
        return type;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public String createMessage(FieldType fieldType) {
        return type.name().toLowerCase() + "," + ibRequestId + "," + fieldType.toCamelCase() + "," + oldValueMap.get(fieldType) + "," + valueMap.get(fieldType);
    }

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

    public int getVolume() {
        return valueMap.get(FieldType.VOLUME).intValue();
    }

    public int getOptionVolume() {
        return valueMap.get(FieldType.OPTION_VOLUME).intValue();
    }

    public int getOptionOpenInterest() {
        return valueMap.get(FieldType.OPTION_OPEN_INTEREST).intValue();
    }
}
