package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.dataholder.ChainDataHolder;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.ib.client.Types;

/**
 * Created by robertk on 12/28/2018.
 */
public class ChainItem {

    private final String id;
    private final double strike;
    private ChainDataHolder call;
    private ChainDataHolder put;

    public ChainItem(double strike) {
        this.strike = strike;
        id = DataHolderType.CHAIN.name().toLowerCase() + "-" + String.valueOf(strike).replace(".", "-");
    }

    public void setupDataHolder(ChainDataHolder dataHolder) {
        Types.Right right = dataHolder.getInstrument().getRight();

        if (right == Types.Right.Call) {
            call = dataHolder;
        } else if (right == Types.Right.Put) {
            put = dataHolder;
        }
    }

    public String getId() {
        return id;
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
