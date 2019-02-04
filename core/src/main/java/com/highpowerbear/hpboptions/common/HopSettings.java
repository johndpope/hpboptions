package com.highpowerbear.hpboptions.common;

import com.highpowerbear.hpboptions.enums.DataField;
import com.highpowerbear.hpboptions.enums.UnderlyingDataField;
import com.highpowerbear.hpboptions.model.RiskThreshold;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 10/28/2018.
 */
public class HopSettings {
    public static final String IB_HOST = "localhost";
    public static final int IB_PORT = 4001;
    public static final int IB_CLIENT_ID = 1;
    public static final int HEARTBEAT_COUNT_INITIAL = 5;
    public static final int UNDERLYING_IB_REQUEST_ID_INITIAL = 0;
    public static final int ORDER_IB_REQUEST_ID_INITIAL = 100000;
    public static final int POSITION_IB_REQUEST_ID_INITIAL = 500000;
    public static final int CHAIN_IB_REQUEST_ID_INITIAL = 1000000;
    public static final int RISK_DATA_UPDATE_INTERVAL_MILLIS = 2000;
    public static final int CHAIN_REBUILD_DELAY_MILLIS = 20000;
    public static final int CHAIN_CONTRACT_DETAILS_REQUEST_WAIT_MILLIS = 6000;
    public static final int CHAIN_STRIKES_STD_DEVIATIONS = 2;
    public static final String WS_TOPIC_PREFIX = "/topic/";
    public static final String EMAIL_FROM = "hpb@highpowerbear.com";
    public static final String EMAIL_TO = "info@highpowerbear.com";
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
    public static final DateTimeFormatter IB_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    public static final DateTimeFormatter IB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter EXCHANGE_RATE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String EXCHANGE_RATES_URL = "http://data.fixer.io/api";
    public static final String EXCHANGE_RATES_SYMBOLS = "EUR,USD,GBP,CHF,AUD,JPY,KRW,HKD,SGD";
    public static final String JMS_DEST_UNDERLYING_RISK_DATA_RECALCULATED = "underlyingRiskDataRecalculated";
    public static final int INVALID_POSITION = -999999999;

    private static final Map<DataField, RiskThreshold> riskThresholdMap = new HashMap<>();
    static {
        riskThresholdMap.put(UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, new RiskThreshold(-30.0, 30.0));
        riskThresholdMap.put(UnderlyingDataField.ALLOCATION_PCT, new RiskThreshold(null, 10.0));
    }
    public static RiskThreshold getRiskThreshold(DataField dataField) {
        return riskThresholdMap.get(dataField);
    }
}
