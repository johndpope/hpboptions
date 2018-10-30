package com.highpowerbear.hpbsystem.common;

import java.time.format.DateTimeFormatter;

/**
 * Created by robertk on 10/28/2018.
 */
public class CoreSettings {
    public static final Integer IB_CONNECT_CLIENT_ID = 1;
    public static final Integer MAX_ORDER_HEARTBEAT_FAILS = 5;
    public static final String JSON_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final DateTimeFormatter JSON_DATE_FORMATTER = DateTimeFormatter.ofPattern(JSON_DATE_FORMAT);
    public static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss.SSS");
    public static final String EMAIL_FROM = "hpb@highpowerbear.com";
    public static final String EMAIL_TO = "info@highpowerbear.com";
}
