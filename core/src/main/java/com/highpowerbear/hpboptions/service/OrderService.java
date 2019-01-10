package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OrderService extends AbstractDataService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final RiskService riskService;
    private final ChainService chainService;

    private final Map<Integer, OrderDataHolder> orderMap = new TreeMap<>(); // sorted by orderId
    private final Map<Integer, OrderDataHolder> contractDetailsRequestMap = new ConcurrentHashMap<>(); // ib request id -> order data holder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.ORDER_IB_REQUEST_ID_INITIAL);
    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    @Autowired
    public OrderService(IbController ibController, HopDao hopDao, MessageService messageService, RiskService riskService, ChainService chainService) {
        super(ibController, hopDao, messageService);
        this.riskService = riskService;
        this.chainService = chainService;

        ibController.addConnectionListener(this);
    }

    @Override
    public void postConnect() {
        ibController.requestOpenOrders();
        cancelAllMktData();
        orderMap.values().forEach(this::requestMktData);
    }

    public void createOrderFromPosition(int conid, Types.Action action) {
        PositionDataHolder pdh = riskService.getPositionDataHolder(conid);

        if (pdh != null) {
            int positionSize = pdh.getPositionSize();
            int quantity;

            if (action == Types.Action.BUY) {
                quantity = positionSize < 0 ? Math.abs(positionSize) : 1;
            } else {
                quantity = positionSize > 0 ? Math.abs(positionSize) : 1;
            }
            createOrder(pdh.getInstrument(), pdh.getInstrument().getMinTick(), pdh.getBid(), pdh.getAsk(), action, quantity);
        } else {
            log.warn("cannot create order from position, no conid " + conid + " found");
        }
    }

    public void createOrderFromChain(int conid, Types.Action action) {
        ChainDataHolder cdh = chainService.getChainDataHolder(conid);

        if (cdh != null) {
            int quantity = 1;
            createOrder(cdh.getInstrument(), cdh.getInstrument().getMinTick(), cdh.getBid(), cdh.getAsk(), action, quantity);
        } else {
            log.warn("cannot create order from chain, no conid " + conid + " found");
        }
    }

    private void createOrder(OptionInstrument instrument, double minTick, double bid, double ask, Types.Action action, int quantity) {
        int orderId = ibOrderIdGenerator.incrementAndGet();
        double limitPrice = Double.NaN;

        if (HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
            RoundingMode roundingMode = action == Types.Action.BUY ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
            int priceScale = BigDecimal.valueOf(minTick).scale();

            limitPrice = BigDecimal.valueOf((bid + ask) / 2d).setScale(priceScale, roundingMode).doubleValue();

            if (action == Types.Action.BUY && limitPrice >= ask) {
                limitPrice -= minTick;
            } else if (action == Types.Action.SELL && limitPrice <= bid) {
                limitPrice += minTick;
            }
        }

        HopOrder hopOrder = new HopOrder(orderId, action, OrderType.LMT);
        hopOrder.setQuantity(quantity);
        hopOrder.setLimitPrice(limitPrice);

        OrderDataHolder odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);
        orderMap.put(orderId, odh);
        requestMktData(odh);
    }

    public void submitOrder(int orderId, int quantity, double limitPrice) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isNew()) {
                placeOrder(hopOrder, odh.getInstrument(), quantity, limitPrice);
            } else {
                log.warn("order not new " + orderId + ", cannot submit");
            }
        }
    }

    public void modifyOrder(int orderId, int quantity, double limitPrice) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isActive()) {
                placeOrder(hopOrder, odh.getInstrument(), quantity, limitPrice);
            } else {
                log.warn("order not active " + orderId + ", cannot modify");
            }
        }
    }

    public void modifyOrderToMoreAggressive(int orderId) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isActive()) {
                double minTick = odh.getInstrument().getMinTick();
                Types.Action action = odh.getHopOrder().getAction();

                double limitPrice = odh.getHopOrder().getLimitPrice() + (action == Types.Action.BUY ? minTick : -minTick);
                odh.getHopOrder().setLimitPrice(limitPrice);

                ibController.placeOrder(hopOrder, odh.getInstrument());
            } else {
                log.warn("order not active " + orderId + ", cannot modifyOrderToMoreAggressive");
            }
        }
    }

    private void placeOrder(HopOrder hopOrder, Instrument instrument, int quantity, double limitPrice) {
        if (quantity > 0 && !Double.isNaN(limitPrice)) {
            hopOrder.setQuantity(quantity);
            hopOrder.setLimitPrice(limitPrice);

            ibController.placeOrder(hopOrder, instrument);
        }
    }

    public void cancelOrder(int orderId) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isActive()) {
                ibController.cancelOrder(orderId);
            } else {
                log.warn("order not active " + orderId + ", cannot cancel");
            }
        }
    }

    public void discardOrder(int orderId) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null && odh.getHopOrder().isNew()) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isNew()) {
                orderMap.remove(orderId);
                cancelMktData(odh);
            } else {
                log.warn("order not new " + orderId + ", cannot discard");
            }
        }
    }

    public void removeCompletedOrders() {
        log.info("removing completed orders");

        new ArrayList<>(orderMap.values()).stream().filter(odh -> odh.getHopOrder().isCompleted()).forEach(odh -> {
            orderMap.remove(odh.getHopOrder().getOrderId());
            cancelMktData(odh);
        });
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
            HopOrder hopOrder = new HopOrder(orderId, order.action(), order.orderType());
            hopOrder.setPermId(order.permId());
            hopOrder.setQuantity((int) order.totalQuantity());
            hopOrder.setLimitPrice(order.lmtPrice());
            hopOrder.setHeartbeatCount(HopSettings.HEARTBEAT_COUNT_INITIAL);

            int conid = contract.conid();
            Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
            String symbol = contract.localSymbol();
            Currency currency = Currency.valueOf(contract.currency());
            Types.Right right = contract.right();
            double strike = contract.strike();
            LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), HopSettings.IB_DATE_FORMATTER);
            int multiplier = Integer.valueOf(contract.multiplier());
            String underlyingSymbol = contract.symbol();

            OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expiration, multiplier, underlyingSymbol);
            odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);
            orderMap.put(orderId, odh);

            int requestId = ibRequestIdGen.incrementAndGet();
            contractDetailsRequestMap.put(requestId, odh);

            ibController.requestContractDetails(requestId, contract);
        }
    }

    @Override
    public void contractDetailsReceived(int requestId, ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();
        OrderDataHolder odh = contractDetailsRequestMap.get(requestId);
        contractDetailsRequestMap.remove(requestId);

        Exchange exchange = Exchange.valueOf(contract.exchange());
        double minTick = contractDetails.minTick();
        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        OptionInstrument instrument = odh.getInstrument();
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        requestMktData(odh);
        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
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