package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.model.HopOrder;
import com.highpowerbear.hpboptions.model.Instrument;
import com.ib.client.Contract;
import com.ib.client.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OrderService extends AbstractDataService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final Map<Integer, HopOrder> orderMap = new HashMap<>();

    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    @Autowired
    public OrderService(IbController ibController, HopDao hopDao, MessageService messageService) {
        super(ibController, hopDao, messageService);

        ibController.addConnectionListener(this);
    }

    @Override
    public void postConnect() {
        ibController.requestOpenOrders();
    }

    public HopOrder createOrder(Instrument instrument) {
        HopOrder hopOrder = new HopOrder(ibOrderIdGenerator.incrementAndGet());
        // TODO

        // TODO send ws message
        return null;
    }

    public void submitOrder(HopOrder hopOrder) {
        // TODO
    }

    public void modifyOrder(HopOrder hopOrder) {
        // TODO
    }

    public void cancelOrder(HopOrder hopOrder) {
        // TODO
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    private void updateHeartbeats() {
        List<HopOrder> activeOrders = orderMap.values().stream().filter(HopOrder::isActive).collect(Collectors.toList());

        for (HopOrder hopOrder : activeOrders) {
            if (hopOrder.getHeartbeatCount() <= 0) {
                hopOrder.setIbStatus(OrderStatus.Unknown);
            } else {
                hopOrder.setHeartbeatCount(hopOrder.getHeartbeatCount() - 1);
            }
            // TODO send ws message
        }
    }

    public void nextValidIdReceived(int nextValidOrderId) {
        ibOrderIdGenerator.set(Math.max(nextValidOrderId, orderMap.keySet().stream().reduce(Integer::max).orElse(0)));
    }

    public void openOrderReceived(int orderId, Contract contract, com.ib.client.Order order) {
        HopOrder hopOrder = orderMap.get(order.permId());

        if (hopOrder == null) { // potential open orders received upon application restart
            hopOrder = new HopOrder(orderId);

            hopOrder.setPermId(order.permId());
            hopOrder.setAction(order.action());
            hopOrder.setQuantity((int) order.totalQuantity());
            hopOrder.setOrderType(order.orderType());
            hopOrder.setLimitPrice(order.lmtPrice());
            hopOrder.setHeartbeatCount(HopSettings.HEARTBEAT_COUNT_INITIAL);
            hopOrder.setInstrument(new Instrument(contract.conid(), contract.secType(), contract.localSymbol(), Currency.valueOf(contract.currency())));

            orderMap.put(hopOrder.getOrderId(), hopOrder);
            // TODO subscribe mkt data
            // TODO send ws message
        }
    }

    public void orderStatusReceived(String status, double remaining, double avgFillPrice, int permId) {
        HopOrder hopOrder = orderMap.get(permId);
        if (hopOrder == null) {
            return;
        }

        hopOrder.setIbStatus(OrderStatus.get(status));
        hopOrder.setHeartbeatCount(HopSettings.HEARTBEAT_COUNT_INITIAL);

        if (OrderStatus.get(status) == OrderStatus.Filled && remaining == 0) {
            hopOrder.setFillPrice(avgFillPrice);
        }
        // TODO send ws message
    }

    public List<HopOrder> getSortedOrders() {
        return orderMap.values().stream()
                .sorted(Comparator.comparingInt(HopOrder::getOrderId))
                .collect(Collectors.toList());
    }
}