package com.highpowerbear.hpboptions.rest.model;

/**
 * Created by robertk on 1/10/2019.
 */
public class SendOrderParams {
    private int orderId;
    private int quantity;
    private double limitPrice;
    private boolean chase;

    public int getOrderId() {
        return orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public boolean isChase() {
        return chase;
    }
}
