package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.model.HopOrder;
import com.highpowerbear.hpboptions.model.Instrument;
import com.highpowerbear.hpboptions.model.OrderDataHolder;
import com.ib.client.Contract;
import com.ib.client.OrderStatus;
import com.ib.client.OrderType;
import com.ib.client.Types;
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

    private final Map<Integer, OrderDataHolder> orderMap = new TreeMap<>(); // sorted by orderId

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.ORDER_IB_REQUEST_ID_INITIAL);
    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    @Autowired
    public OrderService(IbController ibController, HopDao hopDao, MessageService messageService) {
        super(ibController, hopDao, messageService);

        ibController.addConnectionListener(this);
    }

    @Override
    public void postConnect() {
        ibController.requestOpenOrders();
        cancelAllMktData();
        orderMap.values().forEach(this::requestMktData);
    }

    public void createOrder(Types.Action action, int quantity, double limitPrice, Instrument instrument) {
        int orderId = ibOrderIdGenerator.incrementAndGet();

        HopOrder hopOrder = new HopOrder(orderId);
        hopOrder.setAction(action);
        hopOrder.setQuantity(quantity);
        hopOrder.setOrderType(OrderType.LMT);
        hopOrder.setLimitPrice(limitPrice);

        OrderDataHolder odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);
        orderMap.put(orderId, odh);
        requestMktData(odh);

        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
    }

    public void placeOrder(int orderId, int quantity, double limitPrice) { // submit or modify
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            odh.getHopOrder().setQuantity(quantity);
            odh.getHopOrder().setLimitPrice(limitPrice);

            ibController.placeOrder(odh.getHopOrder(), odh.getInstrument());
        }
    }

    public void cancelOrder(int orderId) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            ibController.cancelOrder(odh.getHopOrder().getOrderId());
        }
    }

    public void removeInactiveOrders() {
        for (OrderDataHolder odh : new ArrayList<>(orderMap.values())) {
            if (!odh.getHopOrder().isActive()) {
                orderMap.remove(odh.getHopOrder().getOrderId());
                cancelMktData(odh);
            }
        }
        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    private void updateHeartbeats() {
        List<HopOrder> activeOrders = orderMap.values().stream().map(OrderDataHolder::getHopOrder).filter(HopOrder::isActive).collect(Collectors.toList());

        for (HopOrder hopOrder : activeOrders) {
            if (hopOrder.getHeartbeatCount() <= 0) {
                hopOrder.setIbStatus(OrderStatus.Unknown);
            } else {
                hopOrder.setHeartbeatCount(hopOrder.getHeartbeatCount() - 1);
            }
        }
        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
    }

    public void nextValidIdReceived(int nextValidOrderId) {
        ibOrderIdGenerator.set(Math.max(nextValidOrderId, orderMap.keySet().stream().reduce(Integer::max).orElse(0)));
    }

    public void openOrderReceived(int orderId, Contract contract, com.ib.client.Order order) {
        OrderDataHolder odh = orderMap.get(orderId);

        // potential open orders received upon application restart
        if (odh == null) {
            HopOrder hopOrder = new HopOrder(orderId);
            hopOrder.setPermId(order.permId());
            hopOrder.setAction(order.action());
            hopOrder.setQuantity((int) order.totalQuantity());
            hopOrder.setOrderType(order.orderType());
            hopOrder.setLimitPrice(order.lmtPrice());
            hopOrder.setHeartbeatCount(HopSettings.HEARTBEAT_COUNT_INITIAL);

            Instrument instrument = new Instrument(contract.conid(), contract.secType(), contract.localSymbol(), Currency.valueOf(contract.currency()));

            odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);

            orderMap.put(hopOrder.getOrderId(), odh);
            requestMktData(odh);
            messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
        }
    }

    public void orderStatusReceived(int orderId, String status, double remaining, double avgFillPrice) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();
            hopOrder.setIbStatus(OrderStatus.get(status));
            hopOrder.setHeartbeatCount(HopSettings.HEARTBEAT_COUNT_INITIAL);

            if (OrderStatus.get(status) == OrderStatus.Filled && remaining == 0) {
                hopOrder.setFillPrice(avgFillPrice);
            }
            messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
        }
    }

    public Collection<OrderDataHolder> getOrderDataHolders() {
        return orderMap.values();
    }
}