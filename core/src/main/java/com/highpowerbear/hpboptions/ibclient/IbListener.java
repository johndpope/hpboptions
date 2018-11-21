package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.dao.ProcessDao;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.highpowerbear.hpboptions.process.HeartbeatMonitor;
import com.highpowerbear.hpboptions.process.OpenOrderHandler;
import com.highpowerbear.hpboptions.process.RealtimeDataController;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_ORDER;

/**
 *
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbListener extends GenericIbListener {

    private final ProcessDao processDao;
    private final OpenOrderHandler openOrderHandler;
    private final IbController ibController;
    private final HeartbeatMonitor heartbeatMonitor;
    private final RealtimeDataController realtimeDataController;
    private final MessageSender messageSender;

    @Autowired
    public IbListener(ProcessDao processDao, OpenOrderHandler openOrderHandler, IbController ibController, HeartbeatMonitor heartbeatMonitor, RealtimeDataController realtimeDataController, MessageSender messageSender) {
        this.processDao = processDao;
        this.openOrderHandler = openOrderHandler;
        this.ibController = ibController;
        this.heartbeatMonitor = heartbeatMonitor;
        this.realtimeDataController = realtimeDataController;
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

        IbOrder ibOrder = processDao.getIbOrderByPermId((long) permId);
        if (ibOrder == null) {
            return;
        }

        if ((OrderStatus.SUBMITTED.getIbStatus().equals(status) || OrderStatus.PRESUBMITTED.getIbStatus().equals(status)) && OrderStatus.SUBMITTED.equals(ibOrder.getStatus())) {
            heartbeatMonitor.initHeartbeat(ibOrder);

        } else if (OrderStatus.FILLED.getIbStatus().equals(status) && remaining == 0 && !OrderStatus.FILLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.FILLED, avgFillPrice);
            processDao.updateIbOrder(ibOrder);
            heartbeatMonitor.removeHeartbeat(ibOrder);

        } else if (OrderStatus.CANCELLED.getIbStatus().equals(status) && !OrderStatus.CANCELLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.CANCELLED, null);
            processDao.updateIbOrder(ibOrder);
            heartbeatMonitor.removeHeartbeat(ibOrder);
        }
        messageSender.sendWsMessage(WS_TOPIC_ORDER, "order status changed");
    }

    @Override
    public void managedAccounts(String accountsList) {
        super.managedAccounts(accountsList);
        ibController.getIbConnection().setAccounts(accountsList);
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
        realtimeDataController.tickPriceReceived(tickerId, field, price);
    }

    @Override
    public void tickSize(int tickerId, int field, int size) {
        realtimeDataController.tickSizeReceived(tickerId, field, size);
    }

    @Override
    public void tickGeneric(int tickerId, int tickType, double value) {
        realtimeDataController.tickGenericReceived(tickerId, tickType, value);
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
