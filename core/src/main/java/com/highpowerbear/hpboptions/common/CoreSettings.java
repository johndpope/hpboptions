package com.highpowerbear.hpboptions.common;

import java.time.format.DateTimeFormatter;

/**
 * Created by robertk on 10/28/2018.
 */
public class CoreSettings {
    public static final String IB_HOST = "localhost";
    public static final int IB_PORT = 4001;
    public static final Integer IB_CLIENT_ID = 1;
    public static final Integer MAX_ORDER_HEARTBEAT_FAILS = 5;
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final DateTimeFormatter JSON_DATE_FORMATTER = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT);
    public static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS");
    public static final String EMAIL_FROM = "hpb@highpowerbear.com";
    public static final String EMAIL_TO = "info@highpowerbear.com";
    public static final String WS_TOPIC_ORDER = "/topic/order";
    public static final String WS_TOPIC_UNDERLYING = "/topic/underlying";
    public static final String WS_TOPIC_POSITION = "/topic/position";
    public static final String WS_TOPIC_CHAIN = "/topic/chain";
    public static final String WS_TOPIC_IB_CONNECTION = "/topic/ib_connection";
}
