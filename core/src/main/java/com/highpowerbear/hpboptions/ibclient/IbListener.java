package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.highpowerbear.hpboptions.corelogic.HeartbeatMonitor;
import com.highpowerbear.hpboptions.corelogic.OpenOrderHandler;
import com.highpowerbear.hpboptions.corelogic.DataController;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.SocketException;

import static com.highpowerbear.hpboptions.common.CoreSettings.*;

/**
 *
 * Created by robertk on 11/5/2018.
 */
@Component
@Scope("prototype")
public class IbListener extends GenericIbListener {

    private final CoreDao coreDao;
    private final OpenOrderHandler openOrderHandler;
    private final IbController ibController;
    private final HeartbeatMonitor heartbeatMonitor;
    private final DataController dataController;
    private final MessageSender messageSender;

    @Autowired
    public IbListener(CoreDao coreDao, OpenOrderHandler openOrderHandler, IbController ibController, HeartbeatMonitor heartbeatMonitor, DataController dataController, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.openOrderHandler = openOrderHandler;
        this.ibController = ibController;
        this.heartbeatMonitor = heartbeatMonitor;
        this.dataController = dataController;
        this.messageSender = messageSender;
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
    public void managedAccounts(String accountsList) {
        super.managedAccounts(accountsList);
        ibController.getIbConnection().setAccounts(accountsList);
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        dataController.updateValue(tickerId, field, price);
    }

    @Override
    public void tickSize(int tickerId, int field, int size) {
        dataController.updateValue(tickerId, field, size);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        dataController.updateValue(tickerId, tickType, value);
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
