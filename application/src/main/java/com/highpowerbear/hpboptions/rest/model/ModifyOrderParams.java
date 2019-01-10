package com.highpowerbear.hpboptions.rest.model;

/**
 * Created by robertk on 1/10/2019.
 */
public class ModifyOrderParams {
    private int quantity;
    private double limitPrice;

    public int getQuantity() {
        return quantity;
    }

    public double getLimitPrice() {
        return limitPrice;
    }
}
