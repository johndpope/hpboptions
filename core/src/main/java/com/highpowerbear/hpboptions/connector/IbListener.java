package com.highpowerbear.hpboptions.connector;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.service.MessageService;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.highpowerbear.hpboptions.service.*;
import com.ib.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.SocketException;
import java.util.Set;

/**
 *
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbListener extends GenericIbListener {

    private final IbController ibController;
    private final AccountService accountService;
    private final ActiveUnderlyingService activeUnderlyingService;
    private final LinearService linearService;
    private final OrderService orderService;
    private final PositionService positionService;
    private final ChainService chainService;
    private final ScannerService scannerService;
    private final MessageService messageService;

    @Autowired
    public IbListener(IbController ibController, AccountService accountService, ActiveUnderlyingService activeUnderlyingService, LinearService linearService,
                      OrderService orderService, PositionService positionService, ChainService chainService, ScannerService scannerService, MessageService messageService) {
        this.ibController = ibController;
        this.accountService = accountService;
        this.activeUnderlyingService = activeUnderlyingService;
        this.linearService = linearService;
        this.orderService = orderService;
        this.positionService = positionService;
        this.chainService = chainService;
        this.scannerService = scannerService;
        this.messageService = messageService;

        ibController.initialize(this);
    }

    @Override
    public void error(Exception e) {
        super.error(e);
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
            messageService.sendWsReloadRequestMessage(WsTopic.IB_CONNECTION);
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (errorCode == 507) {
            ibController.connectionBroken();
            messageService.sendWsReloadRequestMessage(WsTopic.IB_CONNECTION);
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        messageService.sendWsReloadRequestMessage(WsTopic.IB_CONNECTION);
    }

    @Override
    public void connectAck() {
        super.connectAck();
        messageService.sendWsReloadRequestMessage(WsTopic.IB_CONNECTION);
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        super.accountSummary(reqId, account, tag, value, currency);
        accountService.accountSummaryReceived(account, tag, value, currency);
    }

    @Override
    public void tickPrice(int requestId, int tickType, double price, TickAttrib attrib) {
        getMarketDataService(requestId).mktDataReceived(requestId, tickType, price);
    }

    @Override
    public void tickSize(int requestId, int tickType, int size) {
        getMarketDataService(requestId).mktDataReceived(requestId, tickType, size);
    }

    @Override
    public void tickGeneric(int requestId, int tickType, double value) {
        getMarketDataService(requestId).mktDataReceived(requestId, tickType, value);
    }

    @Override
    public void tickOptionComputation(int requestId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
        //super.tickOptionComputation(requestId, tickType, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
        getMarketDataService(requestId).optionDataReceived(requestId, TickType.get(tickType), delta, gamma, vega, theta, impliedVol, optPrice, undPrice);
    }

    @Override
    public void historicalData(int requestId, Bar bar) {
        //super.historicalData(requestId, bar);
        if (isUnderlyingIbRequest(requestId)) {
            activeUnderlyingService.historicalDataReceived(requestId, bar);

        } else if (isScannerIbRequest(requestId)) {
            scannerService.historicalDataReceived(requestId, bar);
        }
    }

    @Override
    public void historicalDataEnd(int requestId, String startDateStr, String endDateStr) {
        super.historicalDataEnd(requestId, startDateStr, endDateStr);

        if (isUnderlyingIbRequest(requestId)) {
            activeUnderlyingService.historicalDataEndReceived(requestId);

        } else if (isScannerIbRequest(requestId)) {
            scannerService.historicalDataEndReceived(requestId);
        }
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        //super.position(account, contract, pos, avgCost);
        if (Types.SecType.valueOf(contract.getSecType()) == Types.SecType.OPT) {
            positionService.positionReceived(contract, (int) pos);

        } else if (Types.SecType.valueOf(contract.getSecType()) == Types.SecType.CFD) {
            activeUnderlyingService.positionReceived(contract, (int) pos);

        } else {
            linearService.positionReceived(contract, (int) pos);
        }
    }

    @Override
    public void securityDefinitionOptionalParameter(int requestId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {
        //super.securityDefinitionOptionalParameter(requestId, exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
        chainService.expirationsReceived(underlyingConId, exchange, Integer.valueOf(multiplier), expirations);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int requestId) {
        super.securityDefinitionOptionalParameterEnd(requestId);
        chainService.expirationsEndReceived(requestId);
    }

    @Override
    public void contractDetails(int requestId, ContractDetails contractDetails) {
        //super.contractDetails(requestId, contractDetails);
        getMarketDataService(requestId).contractDetailsReceived(requestId, contractDetails);
    }

    @Override
    public void	contractDetailsEnd(int requestId) {
        super.contractDetailsEnd(requestId);
        getMarketDataService(requestId).contractDetailsEndReceived(requestId);
    }

    @Override
    public void pnlSingle(int requestId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        //super.pnlSingle(requestId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);
        if (isPositionIbRequest(requestId)) {
            positionService.unrealizedPnlReceived(requestId, unrealizedPnL);

        } else if (isUnderlyingIbRequest(requestId)) {
            activeUnderlyingService.unrealizedPnlReceived(requestId, unrealizedPnL);

        } else if (isLinearIbRequest(requestId)) {
            linearService.unrealizedPnlReceived(requestId, unrealizedPnL);
        }
    }

    @Override
    public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {
        //super.pnl(reqId, dailyPnL, unrealizedPnL, realizedPnL);
        accountService.unrealizedPnlReceived(reqId, unrealizedPnL);
    }

    @Override
    public void nextValidId(int orderId) {
        super.nextValidId(orderId);
        orderService.nextValidIdReceived(orderId);
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        super.openOrder(orderId, contract, order, orderState);
        orderService.openOrderReceived(orderId, contract, order);
    }

    @Override
    public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        super.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        orderService.orderStatusReceived(orderId, status, remaining, avgFillPrice);
    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {
        super.scannerData(reqId, rank, contractDetails, distance, benchmark, projection, legsStr);
        // TODO
    }

    @Override
    public void scannerDataEnd(int reqId) {
        super.scannerDataEnd(reqId);
        // TODO
    }

    private MarketDataService getMarketDataService(int requestId) {
        if (isUnderlyingIbRequest(requestId)) {
            return activeUnderlyingService;
        } else if (isOrderIbRequest(requestId)) {
            return orderService;
        } else if (isPositionIbRequest(requestId)) {
            return positionService;
        } else if (isChainIbRequest(requestId)) {
            return chainService;
        } else if (isScannerIbRequest(requestId)) {
            return scannerService;
        } else if (isLinearIbRequest(requestId)) {
            return linearService;
        } else {
            throw new IllegalStateException("no market data service for requestId " + requestId);
        }
    }

    private boolean isUnderlyingIbRequest(int requestId) {
        return requestId >= HopSettings.UNDERLYING_IB_REQUEST_ID_INITIAL && requestId < HopSettings.ORDER_IB_REQUEST_ID_INITIAL;
    }

    private boolean isOrderIbRequest(int requestId) {
        return requestId >= HopSettings.ORDER_IB_REQUEST_ID_INITIAL && requestId < HopSettings.POSITION_IB_REQUEST_ID_INITIAL;
    }

    private boolean isPositionIbRequest(int requestId) {
        return requestId >= HopSettings.POSITION_IB_REQUEST_ID_INITIAL && requestId < HopSettings.CHAIN_IB_REQUEST_ID_INITIAL;
    }

    private boolean isChainIbRequest(int requestId) {
        return requestId >= HopSettings.CHAIN_IB_REQUEST_ID_INITIAL && requestId < HopSettings.SCANNER_IB_REQUEST_ID_INITIAL;
    }

    private boolean isScannerIbRequest(int requestId) {
        return requestId >= HopSettings.SCANNER_IB_REQUEST_ID_INITIAL && requestId < HopSettings.LINEAR_IB_REQUEST_ID_INITIAL;
    }

    private boolean isLinearIbRequest(int requestId) {
        return requestId >= HopSettings.LINEAR_IB_REQUEST_ID_INITIAL;
    }
}
