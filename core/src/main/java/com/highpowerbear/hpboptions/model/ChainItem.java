package com.highpowerbear.hpboptions.model;

import com.ib.client.Types;

/**
 * Created by robertk on 12/28/2018.
 */
public class ChainItem {
    private final double strike;
    private ChainDataHolder call;
    private ChainDataHolder put;

    public ChainItem(double strike) {
        this.strike = strike;
    }

    public void setupDataHolder(ChainDataHolder dataHolder) {
        Types.Right right = dataHolder.getInstrument().getRight();

        if (right == Types.Right.Call) {
            call = dataHolder;
        } else if (right == Types.Right.Put) {
            put = dataHolder;
        }
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
