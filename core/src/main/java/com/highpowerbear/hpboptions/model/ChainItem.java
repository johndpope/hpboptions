package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 12/24/2018.
 */
public class ChainItem {

    private double strike;
    private final ChainDataHolder call;
    private final ChainDataHolder put;

    public ChainItem(double strike, ChainDataHolder call, ChainDataHolder put) {
        this.strike = strike;
        this.call = call;
        this.put = put;
    }

    public double getStrike() {
        return strike;
    }

    public ChainDataHolder getCall() {
        return call;
    }

    public ChainDataHolder getPut() {
        return put;
    }
}
