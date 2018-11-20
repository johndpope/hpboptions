package com.highpowerbear.hpboptions.enums;

import com.ib.client.TickType;

/**
 * Created by robertk on 11/5/2018.
 */
public enum RealtimeFieldType {
    BID,
    ASK,
    LAST,
    CLOSE,
    BID_SIZE,
    ASK_SIZE,
    LAST_SIZE,
    VOLUME,
    CHANGE_PCT,
    DELTA,
    GAMMA,
    VEGA,
    THETA,
    TIME_VALUE,
    OPEN_INTEREST,
    IV_LAST,
    IV_CHANGE;

    public static RealtimeFieldType getFromTickType(TickType tickType) {
        switch(tickType) {
            case BID: return BID;
            case ASK: return ASK;
            case LAST: return LAST;
            case CLOSE: return CLOSE;
            case BID_SIZE: return BID_SIZE;
            case ASK_SIZE: return ASK_SIZE;
            case LAST_SIZE: return LAST_SIZE;
            case VOLUME: return VOLUME;
            default: return null;
        }
    }
}
