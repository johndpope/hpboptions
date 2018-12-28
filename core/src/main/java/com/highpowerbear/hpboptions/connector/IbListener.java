package com.highpowerbear.hpboptions.connector;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.enums.WsTopic;
import com.highpowerbear.hpboptions.logic.ChainService;
import com.highpowerbear.hpboptions.logic.DataService;
import com.highpowerbear.hpboptions.logic.OrderService;
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
    private final DataService dataService;
    private final OrderService orderService;
    private final ChainService chainService;
    private final MessageSender messageSender;

    @Autowired
    public IbListener(IbController ibController, DataService dataService, OrderService orderService, ChainService chainService, MessageSender messageSender) {
        this.ibController = ibController;
        this.dataService = dataService;
        this.orderService = orderService;
        this.chainService = chainService;
        this.messageSender = messageSender;

        ibController.initialize(this);
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        super.openOrder(orderId, contract, order, orderState);
        orderService.openOrderReceived(orderId, contract, order);
    }

    @Override
    public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        super.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        orderService.orderStatusReceived(status, remaining, avgFillPrice, permId);
    }

    @Override
    public void error(Exception e) {
        super.error(e);
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
            messageSender.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (errorCode == 507) {
            ibController.connectionBroken();
            messageSender.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        messageSender.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
    }

    @Override
    public void connectAck() {
        super.connectAck();
        messageSender.sendWsMessage(WsTopic.IB_CONNECTION, ibController.getConnectionInfo());
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        super.accountSummary(reqId, account, tag, value, currency);

        dataService.accountSummaryReceived(account, tag, value, currency);
    }

    @Override
    public void tickPrice(int requestId, int tickType, double price, TickAttrib attrib) {
        dataService.mktDataReceived(requestId, tickType, price);
    }

    @Override
    public void tickSize(int requestId, int tickType, int size) {
        dataService.mktDataReceived(requestId, tickType, size);
    }

    @Override
    public void tickGeneric(int requestId, int tickType, double value) {
        dataService.mktDataReceived(requestId, tickType, value);
    }

    @Override
    public void tickOptionComputation(int requestId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
        //super.tickOptionComputation(requestId, tickType, impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice);
        dataService.optionDataReceived(requestId, TickType.get(tickType), delta, gamma, vega, theta, impliedVol, optPrice, undPrice);
    }

    @Override
    public void historicalData(int requestId, Bar bar) {
        //super.historicalData(requestId, bar);
        dataService.historicalDataReceived(requestId, bar);
    }

    @Override
    public void historicalDataEnd(int requestId, String startDateStr, String endDateStr) {
        super.historicalDataEnd(requestId, startDateStr, endDateStr);
        dataService.historicalDataEndReceived(requestId);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        super.position(account, contract, pos, avgCost);
        if (Types.SecType.valueOf(contract.getSecType()) != Types.SecType.OPT) {
            return;
        }
        dataService.positionReceived(contract, (int) pos);
    }

    @Override
    public void securityDefinitionOptionalParameter(int requestId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {
        super.securityDefinitionOptionalParameter(requestId, exchange, underlyingConId, tradingClass, multiplier, expirations, strikes);
        chainService.expirationsReceived(underlyingConId, exchange, Integer.valueOf(multiplier), expirations);
    }

    @Override
    public void securityDefinitionOptionalParameterEnd(int requestId) {
        super.securityDefinitionOptionalParameterEnd(requestId);
        chainService.chainsDataEndReceived(requestId);
    }

    @Override
    public void contractDetails(int requestId, ContractDetails contractDetails) {
        if (Types.SecType.valueOf(contractDetails.contract().getSecType()) != Types.SecType.OPT) {
            return;
        }
        if (requestId > CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL) {
            chainService.contractDetailsReceived(contractDetails);
        } else {
            super.contractDetails(requestId, contractDetails);
            dataService.contractDetailsReceived(contractDetails);
        }
    }

    @Override
    public void	contractDetailsEnd(int requestId) {
        super.contractDetailsEnd(requestId);

        if (requestId > CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL) {
            chainService.chainsDataEndReceived(requestId);
        }
    }

    @Override
    public void pnlSingle(int requestId, int pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {
        //super.pnlSingle(requestId, pos, dailyPnL, unrealizedPnL, realizedPnL, value);

        dataService.unrealizedPnlReceived(requestId, unrealizedPnL);
    }
}
