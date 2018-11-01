package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 10/27/2018.
 */
public enum OrderStatus {

    PRESUBMITTED(com.ib.client.OrderStatus.PreSubmitted),
    SUBMITTED(com.ib.client.OrderStatus.Submitted),
    UPDATED(null),
    CANCELLED(com.ib.client.OrderStatus.Cancelled),
    FILLED(com.ib.client.OrderStatus.Filled),
    UNKNOWN(null);

    private final com.ib.client.OrderStatus ibStatus;

    OrderStatus(com.ib.client.OrderStatus ibStatus) {
        this.ibStatus = ibStatus;
    }

    public String getIbStatus() {
        return ibStatus != null ? ibStatus.name() : "";
    }
}