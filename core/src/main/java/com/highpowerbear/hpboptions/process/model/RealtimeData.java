package com.highpowerbear.hpboptions.process.model;

import com.highpowerbear.hpboptions.enums.RealtimeFieldType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 11/5/2018.
 */
public class RealtimeData {
    private String contractInfo;
    private int ibRequestId;

    private Map<RealtimeFieldType, Double> valueMapDouble = new HashMap<>();
    private Map<RealtimeFieldType, Integer> valueMapInteger = new HashMap<>();

    public RealtimeData(String contractInfo, int ibRequestId) {
        this.contractInfo = contractInfo;
        this.ibRequestId = ibRequestId;
        initFields();
    }

    private void initFields() {
        valueMapDouble.put(RealtimeFieldType.BID, Double.NaN);
        valueMapDouble.put(RealtimeFieldType.ASK, Double.NaN);
        valueMapDouble.put(RealtimeFieldType.LAST, Double.NaN);
        valueMapDouble.put(RealtimeFieldType.CLOSE, Double.NaN);
        valueMapDouble.put(RealtimeFieldType.CHANGE_PCT, Double.NaN);

        valueMapInteger.put(RealtimeFieldType.BID_SIZE, -1);
        valueMapInteger.put(RealtimeFieldType.ASK_SIZE, -1);
        valueMapInteger.put(RealtimeFieldType.LAST_SIZE, -1);
        valueMapInteger.put(RealtimeFieldType.VOLUME, -1);
    }

    public String createUpdateMessage(RealtimeFieldType fieldType, double value) {
        valueMapDouble.put(fieldType, value);
        return "rt," + contractInfo + "," + fieldType + "," + value;
    }

    public String createUpdateMessage(RealtimeFieldType fieldType, int value) {
        valueMapInteger.put(fieldType, value);
        return "rt," + contractInfo + "," + fieldType + "," + value;
    }

    public String createUpdateMsgChangePct() {
        Double last = valueMapDouble.get(RealtimeFieldType.LAST);
        Double close = valueMapDouble.get(RealtimeFieldType.CLOSE);
        String message = null;

        if (!last.isNaN() && !close.isNaN()) {
            double value = ((last - close) / close) * 100d;
            message = "rt," + contractInfo + "," + RealtimeFieldType.CHANGE_PCT + "," + value;
        }
        return message;
    }

    public String getContractInfo() {
        return contractInfo;
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public double getBid() {
        return valueMapDouble.get(RealtimeFieldType.BID);
    }

    public double getAsk() {
        return valueMapDouble.get(RealtimeFieldType.ASK);
    }

    public double getLast() {
        return valueMapDouble.get(RealtimeFieldType.LAST);
    }

    public double getClose() {
        return valueMapDouble.get(RealtimeFieldType.CLOSE);
    }

    public double getChangePct() {
        return valueMapDouble.get(RealtimeFieldType.CHANGE_PCT);
    }

    public double getBidSize() {
        return valueMapInteger.get(RealtimeFieldType.BID_SIZE);
    }

    public double getAskSize() {
        return valueMapInteger.get(RealtimeFieldType.ASK_SIZE);
    }

    public double getLastSize() {
        return valueMapInteger.get(RealtimeFieldType.LAST_SIZE);
    }

    public double geVolume() {
        return valueMapInteger.get(RealtimeFieldType.VOLUME);
    }
}
