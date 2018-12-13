package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.ib.client.Contract;
import com.ib.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_ORDER;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final CoreDao coreDao;
    private final HeartbeatMonitor heartbeatMonitor;
    private final MessageSender messageSender;

    @Autowired
    public OrderService(CoreDao coreDao, HeartbeatMonitor heartbeatMonitor, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.heartbeatMonitor = heartbeatMonitor;
        this.messageSender = messageSender;
    }

    public void handleOpenOrder(int orderId, Contract contract, Order order) {
        // TODO
    }

    public void handleOrderStatus(String status, double remaining, double avgFillPrice, int permId) {
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
}
