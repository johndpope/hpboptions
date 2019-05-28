package com.highpowerbear.hpboptions.common;

import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.field.ActiveUnderlyingDataField;
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
    public static final String IB_ALL_ACCOUNTS_STRING = "All";
    public static final int HEARTBEAT_COUNT_INITIAL = 5;
    public static final int ACCOUNT_IB_REQUEST_ID_INITIAL = 0;
    public static final int UNDERLYING_IB_REQUEST_ID_INITIAL = 10000;
    public static final int ORDER_IB_REQUEST_ID_INITIAL = 100000;
    public static final int POSITION_IB_REQUEST_ID_INITIAL = 500000;
    public static final int CHAIN_IB_REQUEST_ID_INITIAL = 1000000;
    public static final int SCANNER_IB_REQUEST_ID_INITIAL = 1800000;
    public static final int LINEAR_IB_REQUEST_ID_INITIAL = 2000000;
    public static final int CHAIN_REBUILD_DELAY_MILLIS = 20000;
    public static final int CHAIN_CONTRACT_DETAILS_REQUEST_WAIT_MILLIS = 6000;
    public static final int CHAIN_STRIKES_STD_DEVIATIONS = 2;
    public static final String WS_TOPIC_PREFIX = "/topic/";
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
    public static final DateTimeFormatter IB_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    public static final DateTimeFormatter IB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter EXCHANGE_RATE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String EXCHANGE_RATES_URL = "http://data.fixer.io/api";
    public static final String EXCHANGE_RATES_SYMBOLS = "EUR,USD,GBP,CHF,AUD,JPY,KRW,HKD,SGD";
    public static final String JMS_DEST_UNDERLYING_RISK_DATA_CALCULATED = "underlyingRiskDataCalculated";
    public static final int OPTION_ORDER_DEFAULT_QUANTITY = 1;
    public static final int CFD_ORDER_DEFAULT_QUANTITY = 10;
    public static final double CFD_MARGIN_FACTOR = 0.2;
    public static final int DELTA_HEDGE_QUANTITY_STEP = 20;
    public static final int DELTA_HEDGE_MIN_INTERVAL_SEC = 60;

    private static final Map<DataField, RiskThreshold> riskThresholdMap = new HashMap<>();
    static {
        riskThresholdMap.put(ActiveUnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT, new RiskThreshold(-50.0, 50.0));
        riskThresholdMap.put(ActiveUnderlyingDataField.PORTFOLIO_GAMMA_ONE_PCT_PCT, new RiskThreshold(-100.0, 100.0));
        riskThresholdMap.put(ActiveUnderlyingDataField.ALLOCATION_PCT, new RiskThreshold(null, 15.0));
    }
    public static RiskThreshold getRiskThreshold(DataField dataField) {
        return riskThresholdMap.get(dataField);
    }
}
