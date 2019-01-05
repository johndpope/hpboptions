package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.ib.client.Contract;
import com.ib.client.Order;
import com.ib.client.OrderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OrderService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final IbController ibController;
    private final CoreDao coreDao;
    private final MessageService messageService;

    private final Map<IbOrder, Integer> openOrderHeartbeatMap = new HashMap<>(); // ibOrder -> number of failed heartbeats left before UNKNOWN

    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    @Autowired
    public OrderService(IbController ibController, CoreDao coreDao, MessageService messageService) {
        this.ibController = ibController;
        this.coreDao = coreDao;
        this.messageService = messageService;

        ibController.addConnectionListener(this);

        coreDao.getOpenIbOrders().forEach(this::initHeartbeat);
    }

    @Override
    public void postConnect() {
        ibController.requestOpenOrders();
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    private void updateHeartbeats() {
        Set<IbOrder> ibOrders = new HashSet<>(openOrderHeartbeatMap.keySet());

        for (IbOrder ibOrder : ibOrders) {
            Integer failedHeartbeatsLeft = openOrderHeartbeatMap.get(ibOrder);

            if (failedHeartbeatsLeft <= 0) {
                if (!OrderStatus.UNKNOWN.equals(ibOrder.getStatus())) {
                    ibOrder.addEvent(OrderStatus.UNKNOWN, null);
                    coreDao.updateIbOrder(ibOrder);
                }
                openOrderHeartbeatMap.remove(ibOrder);
            } else {
                openOrderHeartbeatMap.put(ibOrder, failedHeartbeatsLeft - 1);
            }
        }
    }

    private void initHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.put(ibOrder, CoreSettings.MAX_ORDER_HEARTBEAT_FAILS);
    }

    private void removeHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.remove(ibOrder);
    }

    public void openOrderReceived(int orderId, Contract contract, Order order) {
        String underlyingSymbol = contract.symbol();
        String symbol = contract.localSymbol();

        IbOrder ibOrder = coreDao.getIbOrderByPermId(order.permId());

        if (ibOrder != null) { // update existing
            if (ibOrder.getOrderId() == 0) {
                ibOrder.setOrderId(order.orderId());
                coreDao.updateIbOrder(ibOrder);
            }
            if (ibOrder.getStatus() != OrderStatus.SUBMITTED && ibOrder.getStatus() != OrderStatus.UPDATED) {
                return;
            }

            if (order.orderType() == OrderType.LMT && ibOrder.getOrderPrice() != order.lmtPrice()) {
                ibOrder.setOrderPrice(order.lmtPrice());
                ibOrder.addEvent(OrderStatus.UPDATED, ibOrder.getOrderPrice());
                coreDao.updateIbOrder(ibOrder);

            } else if (order.orderType() == OrderType.STP && ibOrder.getOrderPrice() != order.auxPrice()) {
                ibOrder.setOrderPrice(order.auxPrice());
                ibOrder.addEvent(OrderStatus.UPDATED, ibOrder.getOrderPrice());
                coreDao.updateIbOrder(ibOrder);
            }

        } else { // new order
            ibOrder = new IbOrder();

            ibOrder.setPermId((long) order.permId());
            ibOrder.setOrderId(orderId);
            ibOrder.setAction(order.action());
            ibOrder.setQuantity((int) order.totalQuantity());
            ibOrder.setUnderlyingSymbol(underlyingSymbol);
            ibOrder.setCurrency(Currency.valueOf(contract.currency()));
            ibOrder.setSymbol(symbol);
            ibOrder.setSecType(contract.secType());
            ibOrder.setOrderType(order.orderType());

            if (order.orderType() == OrderType.LMT) {
                ibOrder.setOrderPrice(order.lmtPrice());
            } else if (order.orderType() == OrderType.STP) {
                ibOrder.setOrderPrice(order.auxPrice());
            }

            ibOrder.addEvent(OrderStatus.SUBMITTED, ibOrder.getOrderPrice());
            coreDao.createIbOrder(ibOrder);
            initHeartbeat(ibOrder);
        }
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
            initHeartbeat(ibOrder);

        } else if (OrderStatus.FILLED.getIbStatus().equals(status) && remaining == 0 && !OrderStatus.FILLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.FILLED, avgFillPrice);
            coreDao.updateIbOrder(ibOrder);
            removeHeartbeat(ibOrder);

        } else if (OrderStatus.CANCELLED.getIbStatus().equals(status) && !OrderStatus.CANCELLED.equals(ibOrder.getStatus())) {
            ibOrder.addEvent(OrderStatus.CANCELLED, null);
            coreDao.updateIbOrder(ibOrder);
            removeHeartbeat(ibOrder);
        }
        messageService.sendWsMessage(WsTopic.ORDER, "order status changed");
    }
}
