package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 12/30/2018.
 */
public class UnderlyingInfo {

    private final int conid;
    private final String symbol;

    public UnderlyingInfo(int conid, String symbol) {
        this.conid = conid;
        this.symbol = symbol;
    }

    public int getConid() {
        return conid;
    }

    public String getSymbol() {
        return symbol;
    }
}
