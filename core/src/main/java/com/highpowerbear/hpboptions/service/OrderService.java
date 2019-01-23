package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.enums.Currency;
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

    private final PositionService positionService;
    private final ChainService chainService;

    private final Map<Integer, OrderDataHolder> orderMap = new TreeMap<>(); // orderId -> order data holder (sorted by orderId)
    private final Map<Integer, OrderDataHolder> contractDetailsRequestMap = new ConcurrentHashMap<>(); // ib request id -> order data holder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.ORDER_IB_REQUEST_ID_INITIAL);
    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    private OrderFilter orderFilter = new OrderFilter();

    @Autowired
    public OrderService(IbController ibController, HopDao hopDao, MessageService messageService, PositionService positionService, ChainService chainService) {
        super(ibController, hopDao, messageService);
        this.positionService = positionService;
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
        PositionDataHolder pdh = positionService.getPositionDataHolder(conid);

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
        log.info("creating order " + action + " " + quantity + " " + instrument.getSymbol() + ", conid=" + instrument.getConid());

        int orderId = ibOrderIdGenerator.incrementAndGet();

        HopOrder hopOrder = new HopOrder(orderId, action, OrderType.LMT);
        hopOrder.setQuantity(quantity);

        if (HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
            hopOrder.setLimitPrice(scaleLimitPrice((bid + ask) / 2d, action, minTick));
        }

        OrderDataHolder odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);
        orderMap.put(orderId, odh);
        requestMktData(odh);

        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
    }

    private double scaleLimitPrice(double limitPrice, Types.Action action, double minTick) {
        RoundingMode roundingMode = action == Types.Action.BUY ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
        int priceScale = BigDecimal.valueOf(minTick).scale();

        return BigDecimal.valueOf(limitPrice).setScale(priceScale, roundingMode).doubleValue();
    }

    // submit new or modify working order
    public void sendOrder(int orderId, int quantity, double limitPrice, boolean chase) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isNew() || hopOrder.isWorking()) {
                OptionInstrument instrument = odh.getInstrument();

                if (HopUtil.isValidSize(quantity) && HopUtil.isValidPrice(limitPrice)) {
                    hopOrder.setQuantity(quantity);
                    limitPrice = scaleLimitPrice(limitPrice, hopOrder.getAction(), instrument.getMinTick());
                    hopOrder.setLimitPrice(limitPrice);
                    hopOrder.setChase(chase);

                    ibController.placeOrder(hopOrder, instrument);
                } else {
                    log.warn("cannot send order " + orderId + ", quantity or limitPrice not valid, quantity=" + quantity + ", limitPrice=" + limitPrice);
                }
            } else {
                log.warn("cannot send order " + orderId + ", not new or working");
            }
        }
    }

    private void chaseOrder(OrderDataHolder odh, TickType tickType) {
        HopOrder hopOrder = odh.getHopOrder();

        if (hopOrder.isWorking() && hopOrder.isChase()) {
            double minTick = odh.getInstrument().getMinTick();

            Types.Action action = hopOrder.getAction();
            OptionInstrument instrument = odh.getInstrument();

            if (action == Types.Action.BUY && tickType == TickType.ASK && odh.isAskRising()) {
                double limitPrice = scaleLimitPrice(odh.getAsk() - minTick, action, minTick);

                if (limitPrice > hopOrder.getLimitPrice()) {
                    hopOrder.setLimitPrice(limitPrice);
                    ibController.placeOrder(hopOrder, instrument);
                }

            } else if (action == Types.Action.SELL && tickType == TickType.BID && odh.isBidFalling()) {
                double limitPrice = scaleLimitPrice(odh.getBid() + minTick, action, minTick);

                if (limitPrice < hopOrder.getLimitPrice()) {
                    hopOrder.setLimitPrice(limitPrice);
                    ibController.placeOrder(hopOrder, instrument);
                }
            }
        }
    }

    public void cancelOrder(int orderId) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isWorking()) {
                ibController.cancelOrder(orderId);
            } else {
                log.warn("cannot cancel order " + orderId + ", not working");
            }
        }
    }

    public void removeIdleOrders() {
        log.info("removing all idle orders");

        for (OrderDataHolder odh : new ArrayList<>(orderMap.values())) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isIdle()) {
                orderMap.remove(hopOrder.getOrderId());
                cancelMktData(odh);
            }
        }
        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    private void updateHeartbeats() {
        List<HopOrder> activeOrders = orderMap.values().stream().map(OrderDataHolder::getHopOrder).filter(HopOrder::isWorking).collect(Collectors.toList());

        for (HopOrder hopOrder : activeOrders) {
            if (hopOrder.getHeartbeatCount() <= 0) {
                hopOrder.setIbStatus(OrderStatus.Unknown);
            } else {
                hopOrder.setHeartbeatCount(hopOrder.getHeartbeatCount() - 1);
            }
        }
        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
        ibController.requestOpenOrders();
    }

    @Override
    public void mktDataReceived(int requestId, int tickTypeIndex, Number value) {
        super.mktDataReceived(requestId, tickTypeIndex, value);

        TickType tickType = TickType.get(tickTypeIndex);
        if (tickType == TickType.BID || tickType == TickType.ASK) {
            chaseOrder((OrderDataHolder) mktDataRequestMap.get(requestId), tickType);
        }
    }

    public void nextValidIdReceived(int nextValidOrderId) {
        ibOrderIdGenerator.set(Math.max(nextValidOrderId, orderMap.keySet().stream().reduce(Integer::max).orElse(0)));
    }

    public void openOrderReceived(int orderId, Contract contract, Order order) {
        OrderDataHolder odh = orderMap.get(orderId);

        // potential open orders received upon application restart
        if (odh == null) {
            HopOrder hopOrder = new HopOrder(orderId, order.action(), order.orderType());

            hopOrder.setPermId(order.permId());
            hopOrder.setQuantity((int) order.totalQuantity());
            hopOrder.setLimitPrice(order.lmtPrice());

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

        } else {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.getPermId() == null) {
                hopOrder.setPermId(order.permId());
                messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
            }
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

    public OrderFilter getOrderFilter() {
        return orderFilter;
    }

    public void setOrderFilter(OrderFilter orderFilter) {
        this.orderFilter = orderFilter;
    }

    public List<OrderDataHolder> getFilteredOrderDataHolders() {
        List<OrderDataHolder> filteredOrderDataHolders = new ArrayList<>();

        for (OrderDataHolder odh : orderMap.values()) {
            HopOrder hopOrder = odh.getHopOrder();

            if (orderFilter.isShowNew() && hopOrder.isNew()) {
                filteredOrderDataHolders.add(odh);
            } else if (orderFilter.isShowWorking() && hopOrder.isWorking()) {
                filteredOrderDataHolders.add(odh);
            } else if (orderFilter.isShowCompleted() && hopOrder.isCompleted()) {
                filteredOrderDataHolders.add(odh);
            }
        }
        return filteredOrderDataHolders;
    }
}