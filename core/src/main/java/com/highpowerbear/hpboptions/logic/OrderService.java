package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.ib.client.Contract;
import com.ib.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OrderService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final IbController ibController;
    private final CoreDao coreDao;
    private final HeartbeatMonitor heartbeatMonitor;
    private final MessageSender messageSender;

    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    @Autowired
    public OrderService(IbController ibController, CoreDao coreDao, HeartbeatMonitor heartbeatMonitor, MessageSender messageSender) {
        this.ibController = ibController;
        this.coreDao = coreDao;
        this.heartbeatMonitor = heartbeatMonitor;
        this.messageSender = messageSender;

        ibController.addConnectionListener(this);
    }

    @PostConstruct
    public void init() {
        // TODO
    }

    @Override
    public void postConnect() {
        // TODO
    }

    @Override
    public void preDisconnect() {
        // TODO
    }

    public void openOrderReceived(int orderId, Contract contract, Order order) {
        // TODO
    }

    public void orderStatusReceived(String status, double remaining, double avgFillPrice, int permId) {
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
        messageSender.sendWsMessage(WsTopic.ORDER, "order status changed");
    }
}
