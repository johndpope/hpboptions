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
    private final UnderlyingService underlyingService;
    private final OrderService orderService;
    private final PositionService positionService;
    private final ChainService chainService;
    private final MessageService messageService;

    @Autowired
    public IbListener(IbController ibController, UnderlyingService underlyingService, OrderService orderService, PositionService positionService, ChainService chainService, MessageService messageService) {
        this.ibController = ibController;
        this.underlyingService = underlyingService;
        this.orderService = orderService;
        this.positionService = positionService;
        this.chainService = chainService;
        this.messageService = messageService;

        ibController.initialize(this);
    }

    @Override
    public void error(Exception e) {
        super.error(e);
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
            messageService.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (errorCode == 507) {
            ibController.connectionBroken();
            messageService.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        messageService.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
    }

    @Override
    public void connectAck() {
        super.connectAck();
        messageService.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        super.accountSummary(reqId, account, tag, value, currency);
        underlyingService.accountSummaryReceived(account, tag, value, currency);
    }

    @Override
    public void tickPrice(int requestId, int tickType, double price, TickAttrib attrib) {
        getDataService(requestId).mktDataReceived(requestId, tickType, price);
    }

    @Override
    public void tickSize(int requestId, int tickType, int size) {
        getDataService(requestId).mktDataReceived(requestId, tickType, size);
    }

    @Override
    public void tickGeneric(int requestId, int tickType, double value) {
        getDataService(requestId).mktDataReceived(requestId, tickType, value);
    }

    @Override
    public void tickOptionComputation(int requestId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
        //super.tickOptionComputation(requestId, tickType, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
        getDataService(requestId).optionDataReceived(requestId, TickType.get(tickType), delta, gamma, vega, theta, impliedVol, optPrice, undPrice);
    }

    @Override
    public void historicalData(int requestId, Bar bar) {
        //super.historicalData(requestId, bar);
        underlyingService.historicalDataReceived(requestId, bar);
    }

    @Override
    public void historicalDataEnd(int requestId, String startDateStr, String endDateStr) {
        super.historicalDataEnd(requestId, startDateStr, endDateStr);
        underlyingService.historicalDataEndReceived(requestId);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        super.position(account, contract, pos, avgCost);

        if (Types.SecType.valueOf(contract.getSecType()) == Types.SecType.OPT) {
            positionService.positionReceived(contract, (int) pos);

        } else if (Types.SecType.valueOf(contract.getSecType()) == Types.SecType.CFD) {
            // TODO underlyingService.positionReceived
        }
    }

    @Override
    public void securityDefinitionOptionalParameter(int requestId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {
        super.securityDefinitionOptionalParameter(requestId, exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
        chainService.expirationsReceived(underlyingConId, exchange, Integer.valueOf(multiplier), expirations);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int requestId) {
        super.securityDefinitionOptionalParameterEnd(requestId);
        chainService.expirationsEndReceived(requestId);
    }

    @Override
    public void contractDetails(int requestId, ContractDetails contractDetails) {
        if (Types.SecType.valueOf(contractDetails.contract().getSecType()) != Types.SecType.OPT) {
            return;
        }
        //super.contractDetails(requestId, contractDetails);
        getDataService(requestId).contractDetailsReceived(requestId, contractDetails);
    }

    @Override
    public void	contractDetailsEnd(int requestId) {
        super.contractDetailsEnd(requestId);
        getDataService(requestId).contractDetailsEndReceived(requestId);
    }

    @Override
    public void pnlSingle(int requestId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        //super.pnlSingle(requestId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);
        positionService.unrealizedPnlReceived(requestId, unrealizedPnL);
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

    private DataService getDataService(int requestId) {
        if (requestId >= HopSettings.UNDERLYING_IB_REQUEST_ID_INITIAL && requestId < HopSettings.ORDER_IB_REQUEST_ID_INITIAL) {
            return underlyingService;
        } else if (requestId >= HopSettings.ORDER_IB_REQUEST_ID_INITIAL && requestId < HopSettings.POSITION_IB_REQUEST_ID_INITIAL) {
            return orderService;
        } else if (requestId >= HopSettings.POSITION_IB_REQUEST_ID_INITIAL && requestId < HopSettings.CHAIN_IB_REQUEST_ID_INITIAL) {
            return positionService;
        } else {
            return chainService;
        }
    }
}
