package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.dataholder.*;
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
public class OrderService extends AbstractMarketDataService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final ActiveUnderlyingService activeUnderlyingService;
    private final PositionService positionService;
    private final ChainService chainService;

    private final Map<Integer, OrderDataHolder> orderMap = new TreeMap<>(); // orderId -> order data holder (sorted by orderId)
    private final Map<Integer, OrderDataHolder> contractDetailsRequestMap = new ConcurrentHashMap<>(); // ib request id -> order data holder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.ORDER_IB_REQUEST_ID_INITIAL);
    private final AtomicInteger ibOrderIdGenerator = new AtomicInteger();

    private OrderFilter orderFilter = new OrderFilter();

    @Autowired
    public OrderService(IbController ibController, MessageService messageService, ActiveUnderlyingService activeUnderlyingService, PositionService positionService, ChainService chainService) {
        super(ibController, messageService);

        this.activeUnderlyingService = activeUnderlyingService;
        this.positionService = positionService;
        this.chainService = chainService;

        ibController.addConnectionListener(this);
    }

    @Override
    public void postConnect() {
        cancelAllMktData();
        // open orders received after connecting even when not requested, so additional filtering needed
        orderMap.values().stream().filter(odh -> odh.getInstrument().getExchange() != null).forEach(this::requestMktData);
        ibController.requestOpenOrders();
    }

    public void createOrderFromPosition(int conid, Types.Action action) {
        PositionDataHolder pdh = positionService.getPositionDataHolder(conid);

        if (pdh != null) {
            int positionSize = pdh.getPositionSize();
            int quantity;

            if (action == Types.Action.BUY) {
                quantity = positionSize < 0 ? Math.abs(positionSize) : HopSettings.OPTION_ORDER_DEFAULT_QUANTITY;
            } else {
                quantity = positionSize > 0 ? Math.abs(positionSize) : HopSettings.OPTION_ORDER_DEFAULT_QUANTITY;
            }
            createOrder(pdh.getInstrument(), pdh.getInstrument().getMinTick(), pdh.getBid(), pdh.getAsk(), action, quantity, OrderSource.PM);
        } else {
            log.warn("cannot create order from position, no conid " + conid + " found");
        }
    }

    public void createOrderFromChain(int conid, Types.Action action) {
        ChainDataHolder cdh = chainService.getChainDataHolder(conid);

        if (cdh != null) {
            int quantity = HopSettings.OPTION_ORDER_DEFAULT_QUANTITY;
            createOrder(cdh.getInstrument(), cdh.getInstrument().getMinTick(), cdh.getBid(), cdh.getAsk(), action, quantity, OrderSource.CM);
        } else {
            log.warn("cannot create order from chain, no conid " + conid + " found");
        }
    }

    public void createOrderFromUnderlying(int underlyingConid, Types.Action action) {
        ActiveUnderlyingDataHolder udh = activeUnderlyingService.getUnderlyingDataHolder(underlyingConid);

        if (udh != null) {
            Instrument cfdInstrument = udh.getCfdInstrument();

            if (cfdInstrument != null) {
                int positionSize = udh.getCfdPositionSize();
                int quantity;

                if (action == Types.Action.BUY) {
                    quantity = positionSize < 0 ? Math.abs(positionSize) : HopSettings.CFD_ORDER_DEFAULT_QUANTITY;
                } else {
                    quantity = positionSize > 0 ? Math.abs(positionSize) : HopSettings.CFD_ORDER_DEFAULT_QUANTITY;
                }
                createOrder(cfdInstrument, cfdInstrument.getMinTick(), udh.getBid(), udh.getAsk(), action, quantity, OrderSource.UM);
            } else {
                log.warn("cannot create order from underlying conid=" + underlyingConid + ", cfd parameters not defined");
            }
        } else {
            log.warn("cannot create order from underlying, no conid " + underlyingConid + " found");
        }
    }

    public void createAndSendAdaptiveCfdOrder(int underlyingConid, Types.Action action, int quantity, OrderSource orderSource) {
        ActiveUnderlyingDataHolder udh = activeUnderlyingService.getUnderlyingDataHolder(underlyingConid);

        if (udh != null) {
            Instrument cfdInstrument = udh.getCfdInstrument();

            if (cfdInstrument != null) {
                int orderId = createOrder(cfdInstrument, cfdInstrument.getMinTick(), udh.getBid(), udh.getAsk(), action, quantity, orderSource);

                OrderDataHolder odh = orderMap.get(orderId);
                HopOrder hopOrder = odh.getHopOrder();
                hopOrder.setAdapt(true);

                ibController.placeOrder(hopOrder, cfdInstrument);
            } else {
                log.warn("cannot create and send adaptive cfd order for underlying conid=" + underlyingConid + ", cfd parameters not defined");
            }
        } else {
            log.warn("cannot create and send adaptive cfd order for underlying, no conid " + underlyingConid + " found");
        }
    }

    private int createOrder(Instrument instrument, double minTick, double bid, double ask, Types.Action action, int quantity, OrderSource orderSource) {
        log.info("creating order " + action + " " + quantity + " " + instrument.getSymbol() + ", conid=" + instrument.getConid());

        int orderId = ibOrderIdGenerator.incrementAndGet();

        HopOrder hopOrder = new HopOrder(orderId, action, OrderType.LMT, orderSource);
        hopOrder.setQuantity(quantity);

        if (HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
            hopOrder.setLimitPrice(calculateLimitPrice(bid, ask, action, minTick));
        }

        OrderDataHolder odh = new OrderDataHolder(instrument, ibRequestIdGen.incrementAndGet(), hopOrder);
        orderMap.put(orderId, odh);
        requestMktData(odh);

        messageService.sendWsReloadRequestMessage(DataHolderType.ORDER);
        return orderId;
    }

    // submit new or modify working order
    public void sendOrder(int orderId, int quantity, double limitPrice, boolean adapt) {
        OrderDataHolder odh = orderMap.get(orderId);

        if (odh != null) {
            HopOrder hopOrder = odh.getHopOrder();

            if (hopOrder.isNew() || hopOrder.isWorking()) {
                hopOrder.setAdapt(adapt);

                if (HopUtil.isValidSize(quantity) && HopUtil.isValidPrice(limitPrice)) {
                    Instrument instrument = odh.getInstrument();
                    double minTick = instrument.getMinTick();
                    Types.Action action = hopOrder.getAction();

                    double bid = odh.getBid();
                    double ask = odh.getAsk();

                    if (adapt && HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
                        limitPrice = calculateLimitPrice(bid, ask, action, minTick);
                    } else {
                        limitPrice = scaleLimitPrice(limitPrice, action, minTick);
                    }

                    if (hopOrder.isNew() || quantity != hopOrder.getQuantity() || limitPrice != hopOrder.getLimitPrice()) {
                        hopOrder.setQuantity(quantity);
                        hopOrder.setLimitPrice(limitPrice);
                        ibController.placeOrder(hopOrder, instrument);
                    } else {
                        log.info("order " + orderId + " not sent, quantity and limitPrice are the same as before");
                    }
                } else {
                    log.warn("cannot send order " + orderId + ", quantity or limitPrice not valid, quantity=" + quantity + ", limitPrice=" + limitPrice);
                }
            } else {
                log.warn("cannot send order " + orderId + ", not new or working");
            }
        }
    }

    private void adaptOrder(OrderDataHolder odh, TickType tickType) {
        HopOrder hopOrder = odh.getHopOrder();

        if (hopOrder.isWorking() && hopOrder.isAdapt()) {
            Instrument instrument = odh.getInstrument();
            double minTick = instrument.getMinTick();
            Types.Action action = hopOrder.getAction();

            double bid = odh.getBid();
            double ask = odh.getAsk();

            if (HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
                double limitPrice = calculateLimitPrice(bid, ask, action, minTick);

                if (action == Types.Action.BUY && tickType == TickType.ASK && odh.isAskRising() && limitPrice > hopOrder.getLimitPrice()) {
                    hopOrder.setLimitPrice(limitPrice);
                    ibController.placeOrder(hopOrder, instrument);

                } else if (action == Types.Action.SELL && tickType == TickType.BID && odh.isBidFalling() && limitPrice < hopOrder.getLimitPrice()) {
                    hopOrder.setLimitPrice(limitPrice);
                    ibController.placeOrder(hopOrder, instrument);
                }
            }
        }
    }

    private double calculateLimitPrice(double bid, double ask, Types.Action action, double minTick) {
        double limitPrice = scaleLimitPrice((bid + ask) / 2d, action, minTick);

        if (action == Types.Action.BUY && limitPrice == ask) {
            limitPrice = scaleLimitPrice(limitPrice - minTick, action, minTick);

        } else if (action == Types.Action.SELL && limitPrice == bid) {
            limitPrice = scaleLimitPrice(limitPrice + minTick, action, minTick);
        }
        return limitPrice;
    }

    private double scaleLimitPrice(double limitPrice, Types.Action action, double minTick) {
        RoundingMode roundingMode = action == Types.Action.BUY ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN;
        int priceScale = BigDecimal.valueOf(minTick).scale();

        return BigDecimal.valueOf(limitPrice).setScale(priceScale, roundingMode).doubleValue();
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

    public boolean hasWorkingOrder(int conid) {
        return orderMap.values().stream()
                .filter(odh -> odh.getInstrument().getConid() == conid)
                .map(OrderDataHolder::getHopOrder)
                .anyMatch(HopOrder::isWorking);
    }

    @Override
    protected void requestMktData(MarketDataHolder mdh) {
        int requestId = mdh.getIbMktDataRequestId();

        mktDataRequestMap.put(requestId, mdh);

        Contract ibContract;
        if (mdh.getInstrument().getSecType() == Types.SecType.CFD) {
            ActiveUnderlyingDataHolder udh = activeUnderlyingService.getUnderlyingDataHolder(mdh.getInstrument().getUnderlyingConid());
            ibContract = udh.getInstrument().createIbContract();
        } else {
            ibContract = mdh.getInstrument().createIbContract();
        }
        ibController.requestMktData(requestId, ibContract, mdh.getGenericTicks());
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
            adaptOrder((OrderDataHolder) mktDataRequestMap.get(requestId), tickType);
        }
    }

    public void nextValidIdReceived(int nextValidOrderId) {
        ibOrderIdGenerator.set(Math.max(nextValidOrderId, orderMap.keySet().stream().reduce(Integer::max).orElse(0)));
    }

    public void openOrderReceived(int orderId, Contract contract, Order order) {
        OrderDataHolder odh = orderMap.get(orderId);

        // potential open orders received upon application restart
        if (odh == null) {
            HopOrder hopOrder = new HopOrder(orderId, order.action(), order.orderType(), OrderSource.OOR);

            hopOrder.setPermId(order.permId());
            hopOrder.setQuantity((int) order.totalQuantity());
            hopOrder.setLimitPrice(order.lmtPrice());

            int conid = contract.conid();
            Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
            String underlyingSymbol = contract.symbol();
            String symbol = contract.localSymbol();
            Currency currency = Currency.valueOf(contract.currency());

            Instrument instrument;

            if (secType == Types.SecType.OPT) {
                Types.Right right = contract.right();
                double strike = contract.strike();
                LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), HopSettings.IB_DATE_FORMATTER);
                int multiplier = Integer.valueOf(contract.multiplier());
                instrument = new OptionInstrument(conid, secType, underlyingSymbol, symbol, currency, right, strike, expiration, multiplier);
            } else {
                instrument = new Instrument(conid, secType, underlyingSymbol, symbol, currency);
            }

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

        Instrument instrument = odh.getInstrument();
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
