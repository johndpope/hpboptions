package com.highpowerbear.hpboptions.model;

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
    private int permId;
    private Types.Action action;
    private int quantity;
    private OrderType orderType;
    private double limitPrice;
    private double fillPrice;
    private OrderStatus ibStatus;
    private int heartbeatCount;

    public HopOrder(int orderId) {
        this.orderId = orderId;
    }

    public boolean isActive() {
        return ibStatus == null || ibStatus == ApiPending || ibStatus == PendingSubmit || ibStatus == PendingCancel || ibStatus == PreSubmitted || ibStatus == Submitted;
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

    public int getPermId() {
        return permId;
    }

    public void setPermId(int permId) {
        this.permId = permId;
    }

    public Types.Action getAction() {
        return action;
    }

    public void setAction(Types.Action action) {
        this.action = action;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
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

    public OrderStatus getIbStatus() {
        return ibStatus;
    }

    public void setIbStatus(OrderStatus ibStatus) {
        this.ibStatus = ibStatus;
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
