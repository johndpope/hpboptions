package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.HopUtil;

/**
 * Created by robertk on 1/23/2019.
 */
public class UnderlyingMktDataSnapshot {
    private double price;
    private double optionImpliedVol;

    public UnderlyingMktDataSnapshot(double price, double optionImpliedVol) {
        this.price = price;
        this.optionImpliedVol = optionImpliedVol;
    }

    public boolean isValid() {
        return HopUtil.isValidPrice(price) && HopUtil.isValidPrice(optionImpliedVol);
    }

    public double getPrice() {
        return price;
    }

    public double getOptionImpliedVol() {
        return optionImpliedVol;
    }
}
