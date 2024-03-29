package com.highpowerbear.hpboptions.field;

import com.ib.client.TickType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by robertk on 12/3/2018.
 */
public enum BasicMarketDataField implements DataField {
    BID (TickType.BID, null),
    ASK (TickType.ASK, null),
    LAST (TickType.LAST, null),
    CLOSE (TickType.CLOSE, null),
    BID_SIZE (TickType.BID_SIZE, null),
    ASK_SIZE (TickType.ASK_SIZE, null),
    LAST_SIZE (TickType.LAST_SIZE, null),
    VOLUME (TickType.VOLUME, null),
    OPTION_IMPLIED_VOL (TickType.OPTION_IMPLIED_VOL, 106),
    OPTION_CALL_VOLUME (TickType.OPTION_CALL_VOLUME, 100),
    OPTION_PUT_VOLUME (TickType.OPTION_PUT_VOLUME, 100),
    OPTION_CALL_OPEN_INTEREST (TickType.OPTION_CALL_OPEN_INTEREST, 101),
    OPTION_PUT_OPEN_INTEREST (TickType.OPTION_PUT_OPEN_INTEREST, 101);

    private TickType tickType;
    private Integer genericTick;

    BasicMarketDataField(TickType tickType, Integer genericTick) {
        this.tickType = tickType;
        this.genericTick = genericTick;
    }

    public TickType getTickType() {
        return tickType;
    }

    public Integer getGenericTick() {
        return genericTick;
    }

    @Override
    public Number getInitialValue() {
        return -1;
    }

    private static final Map<TickType, BasicMarketDataField> tickFieldMap = Arrays.stream(BasicMarketDataField.values())
            .collect(Collectors.toMap(BasicMarketDataField::getTickType, bf -> bf));

    public static BasicMarketDataField basicField(TickType tickType) {
        return tickFieldMap.get(tickType);
    }

    private static List<BasicMarketDataField> fields = Arrays.asList(BasicMarketDataField.values());

    public static List<BasicMarketDataField> fields() {
        return fields;
    }
}


