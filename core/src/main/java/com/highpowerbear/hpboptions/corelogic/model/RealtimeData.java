package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.FieldType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 11/5/2018.
 */
public class RealtimeData {

    private final Instrument instrument;
    private final int ibRequestId;

    private final Map<FieldType, Double> valueMapDouble = new HashMap<>();
    private final Map<FieldType, Integer> valueMapInteger = new HashMap<>();

    public RealtimeData(Instrument instrument, int ibRequestId) {
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;
        initFields();
    }

    private void initFields() {
        valueMapDouble.put(FieldType.BID, Double.NaN);
        valueMapDouble.put(FieldType.ASK, Double.NaN);
        valueMapDouble.put(FieldType.LAST, Double.NaN);
        valueMapDouble.put(FieldType.CLOSE, Double.NaN);
        valueMapDouble.put(FieldType.CHANGE_PCT, Double.NaN);

        valueMapInteger.put(FieldType.BID_SIZE, -1);
        valueMapInteger.put(FieldType.ASK_SIZE, -1);
        valueMapInteger.put(FieldType.LAST_SIZE, -1);
        valueMapInteger.put(FieldType.VOLUME, -1);
    }

    public String updateValue(FieldType fieldType, double value) {
        valueMapDouble.put(fieldType, value);
        return createMessage(fieldType, String.valueOf(value));
    }

    public String updateValue(FieldType fieldType, int value) {
        valueMapInteger.put(fieldType, value);
        return createMessage(fieldType, String.valueOf(value));
    }

    public String createUpdateMsgChangePct() {
        Double last = valueMapDouble.get(FieldType.LAST);
        Double close = valueMapDouble.get(FieldType.CLOSE);
        String message = null;

        if (!last.isNaN() && !close.isNaN()) {
            double value = ((last - close) / close) * 100d;
            message = createMessage(FieldType.CHANGE_PCT, String.valueOf(value));
        }
        return message;
    }

    private String createMessage(FieldType fieldType, String value) {
        return "rt," + ibRequestId + "," + instrument.getSymbol() + "," + fieldType + "," + value;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public double getBid() {
        return valueMapDouble.get(FieldType.BID);
    }

    public double getAsk() {
        return valueMapDouble.get(FieldType.ASK);
    }

    public double getLast() {
        return valueMapDouble.get(FieldType.LAST);
    }

    public double getClose() {
        return valueMapDouble.get(FieldType.CLOSE);
    }

    public double getChangePct() {
        return valueMapDouble.get(FieldType.CHANGE_PCT);
    }

    public double getBidSize() {
        return valueMapInteger.get(FieldType.BID_SIZE);
    }

    public double getAskSize() {
        return valueMapInteger.get(FieldType.ASK_SIZE);
    }

    public double getLastSize() {
        return valueMapInteger.get(FieldType.LAST_SIZE);
    }

    public double geVolume() {
        return valueMapInteger.get(FieldType.VOLUME);
    }
}
