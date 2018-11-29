package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.CoreService;
import com.highpowerbear.hpboptions.corelogic.HeartbeatMonitor;
import com.highpowerbear.hpboptions.corelogic.OpenOrderHandler;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.SocketException;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_IB_CONNECTION;
import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_ORDER;

/**
 *
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbListener extends GenericIbListener {

    private final CoreDao coreDao;
    private final OpenOrderHandler openOrderHandler;
    private final HeartbeatMonitor heartbeatMonitor;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency
    private CoreService coreService; // prevent circular dependency

    @Autowired
    public IbListener(CoreDao coreDao, OpenOrderHandler openOrderHandler, HeartbeatMonitor heartbeatMonitor, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.openOrderHandler = openOrderHandler;
        this.heartbeatMonitor = heartbeatMonitor;
        this.messageSender = messageSender;
    }

    void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    void setCoreService(CoreService coreService) {
        this.coreService = coreService;
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        super.openOrder(orderId, contract, order, orderState);
        openOrderHandler.handleOpenOrder(orderId, contract, order);
    }

    @Override
    public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
        super.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld);

        if (!(  OrderStatus.SUBMITTED.getIbStatus().equals(status) ||
                OrderStatus.PRESUBMITTED.getIbStatus().equals(status) ||
                OrderStatus.CANCELLED.getIbStatus().equals(status) ||
                OrderStatus.FILLED.getIbStatus().equals(status))) {
            return;
        }

        IbOrder ibOrder = coreDao.getIbOrderByPermId((long) permId);
        if (ibOrder == null) {
            return;
        }

        if ((OrderStatus.SUBMITTED.getIbStatus().equals(status) || OrderStatus.PRESUBMITTED.getIbStatus().equals(status)) && OrderStatus.SUBMITTED.equals(ibOrder.getStatus())) {
            heartbeatMonitor.initHeartbeat(ibOrder);

        } else if (OrderStatus.FILLED.getIbStatus().equals(status) && remaining == 0 && !OrderStatus.FILLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.FILLED, avgFillPrice);
            coreDao.updateIbOrder(ibOrder);
            heartbeatMonitor.removeHeartbeat(ibOrder);

        } else if (OrderStatus.CANCELLED.getIbStatus().equals(status) && !OrderStatus.CANCELLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.CANCELLED, null);
            coreDao.updateIbOrder(ibOrder);
            heartbeatMonitor.removeHeartbeat(ibOrder);
        }
        messageSender.sendWsMessage(WS_TOPIC_ORDER, "order status changed");
    }

    @Override
    public void error(Exception e) {
        super.error(e);
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
            messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (errorCode == 507) {
            ibController.connectionBroken();
            messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
    }

    @Override
    public void connectAck() {
        super.connectAck();
        messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "connected");
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        coreService.updateValue(tickerId, field, price);
    }

    @Override
    public void tickSize(int tickerId, int field, int size) {
        coreService.updateValue(tickerId, field, size);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        coreService.updateValue(tickerId, tickType, value);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        //TODO
    }

    @Override
    public void positionEnd() {
        // TODO
    }
}
