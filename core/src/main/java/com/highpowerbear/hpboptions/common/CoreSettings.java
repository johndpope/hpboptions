package com.highpowerbear.hpboptions.common;

import java.time.format.DateTimeFormatter;

/**
 * Created by robertk on 10/28/2018.
 */
public class CoreSettings {
    public static final String IB_HOST = "localhost";
    public static final int IB_PORT = 4001;
    public static final int IB_CLIENT_ID = 1;
    public static final int MAX_ORDER_HEARTBEAT_FAILS = 5;
    public static final int IB_DATA_REQUEST_ID_INITIAL = 0;
    public static final int IB_CHAIN_REQUEST_ID_INITIAL = 1000000;
    public static final int PORTFOLIO_OPTION_DATA_UPDATE_INTERVAL_MILLIS = 2000;
    public static final int CHAIN_UNDERLYING_DATA_DELAY_MILLIS = 10000;
    public static final int CHAIN_CONTRACT_DETAILS_REQUEST_WAIT_MILLIS = 6000;
    public static final int CHAIN_STRIKES_STD_DEVIATIONS = 2;
    public static final String WS_TOPIC_PREFIX = "/topic/";
    public static final String EMAIL_FROM = "hpb@highpowerbear.com";
    public static final String EMAIL_TO = "info@highpowerbear.com";
    public static final String JSON_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd";
    public static final DateTimeFormatter IB_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
    public static final DateTimeFormatter IB_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
}
