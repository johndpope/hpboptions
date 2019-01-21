package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.enums.HopOrderState;
import com.ib.client.Order;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types;

import static com.ib.client.OrderStatus.*;

/**
 * Created by robertk on 10/27/2018.
 */
public class HopOrder {

    private final int orderId;
    private final Types.Action action;
    private final OrderType orderType;

    private int quantity;
    private double limitPrice;
    private double fillPrice;
    private HopOrderState state;
    private OrderStatus ibStatus;
    private Integer permId;
    private int heartbeatCount;

    public HopOrder(int orderId, Types.Action action, OrderType orderType) {
        this.orderId = orderId;
        this.action = action;
        this.orderType = orderType;

        quantity = 1;
        limitPrice = 0d;
        fillPrice = 0d;
        state = HopOrderState.New;
        ibStatus = null;
        permId = null;
        heartbeatCount = HopSettings.HEARTBEAT_COUNT_INITIAL;
    }

    @JsonIgnore
    public boolean isNew() {
        return state == HopOrderState.New;
    }

    @JsonIgnore
    public boolean isWorking() {
        return state == HopOrderState.Working;
    }

    public Order createIbOrder() {
        Order order = new Order();
        order.action(action);
        order.totalQuantity(quantity);
        order.orderType(orderType);
        order.lmtPrice(limitPrice);
        order.tif(Types.TimeInForce.DAY);

        return order;
    }

    public int getOrderId() {
        return orderId;
    }

    public Types.Action getAction() {
        return action;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(double limitPrice) {
        this.limitPrice = limitPrice;
    }

    public double getFillPrice() {
        return fillPrice;
    }

    public void setFillPrice(double fillPrice) {
        this.fillPrice = fillPrice;
    }

    public HopOrderState getState() {
        return state;
    }

    public OrderStatus getIbStatus() {
        return ibStatus;
    }

    public void setIbStatus(OrderStatus ibStatus) {
        this.ibStatus = ibStatus;

        if (ibStatus == null) {
            state = HopOrderState.New;
        } else if (ibStatus == ApiPending || ibStatus == PendingSubmit || ibStatus == PendingCancel || ibStatus == PreSubmitted || ibStatus == Submitted) {
            state = HopOrderState.Working;
        } else if (ibStatus == ApiCancelled || ibStatus == Cancelled || ibStatus == Filled || ibStatus == Inactive || ibStatus == Unknown) {
            state = HopOrderState.Completed;
        }
    }

    public Integer getPermId() {
        return permId;
    }

    public void setPermId(Integer permId) {
        this.permId = permId;
    }

    public int getHeartbeatCount() {
        return heartbeatCount;
    }

    public void setHeartbeatCount(int heartbeatCount) {
        this.heartbeatCount = heartbeatCount;
    }

    @Override
    public String toString() {
        return orderId + ", " + action + ", " + quantity + ", " + limitPrice;
    }
}
