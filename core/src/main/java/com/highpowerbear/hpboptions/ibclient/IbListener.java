package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.logic.DataService;
import com.highpowerbear.hpboptions.logic.OrderService;
import com.ib.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.SocketException;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_IB_CONNECTION;

/**
 *
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbListener extends GenericIbListener {

    private final OrderService orderService;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency
    private DataService dataService; // prevent circular dependency

    @Autowired
    public IbListener(OrderService orderService, MessageSender messageSender) {
        this.orderService = orderService;
        this.messageSender = messageSender;
    }

    void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {
        super.openOrder(orderId, contract, order, orderState);
        orderService.handleOpenOrder(orderId, contract, order);
    }

    @Override
    public void orderStatus(int orderId, String status, double filled, double remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
        super.orderStatus(orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld, mktCapPrice);
        orderService.handleOrderStatus(status, remaining, avgFillPrice, permId);
    }

    @Override
    public void error(Exception e) {
        super.error(e);
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
            messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
        }
    }

    @Override
    public void error(int id, int errorCode, String errorMsg) {
        super.error(id, errorCode, errorMsg);
        if (errorCode == 507) {
            ibController.connectionBroken();
            messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
        }
    }

    @Override
    public void connectionClosed() {
        super.connectionClosed();
        messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "disconnected");
    }

    @Override
    public void connectAck() {
        super.connectAck();
        messageSender.sendWsMessage(WS_TOPIC_IB_CONNECTION, "connected");
    }

    @Override
    public void tickPrice(int requestId, int tickType, double price, TickAttrib attrib) {
        dataService.updateMktData(requestId, tickType, price);
    }

    @Override
    public void tickSize(int requestId, int tickType, int size) {
        dataService.updateMktData(requestId, tickType, size);
    }

    @Override
    public void tickGeneric(int requestId, int tickType, double value) {
        dataService.updateMktData(requestId, tickType, value);
    }

    @Override
    public void tickOptionComputation(int requestId, int tickType, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {
        dataService.updateOptionData(requestId, tickType, delta, gamma, vega, theta, impliedVol, optPrice, undPrice);
    }

    @Override
    public void historicalData(int requestId, Bar bar) {
        //super.historicalData(requestId, bar);
        dataService.historicalDataReceived(requestId, bar);
    }

    @Override
    public void historicalDataEnd(int requestId, String startDateStr, String endDateStr) {
        super.historicalDataEnd(requestId, startDateStr, endDateStr);
        dataService.historicalDataEnd(requestId);
    }

    @Override
    public void position(String account, Contract contract, double pos, double avgCost) {
        super.position(account, contract, pos, avgCost);
        if (Types.SecType.valueOf(contract.getSecType()) != Types.SecType.OPT) {
            return;
        }
        dataService.optionPositionChanged(contract, (int) pos);
    }

    @Override
    public void contractDetails(int reqId, ContractDetails contractDetails) {
        super.contractDetails(reqId, contractDetails);
        if (Types.SecType.valueOf(contractDetails.contract().getSecType()) != Types.SecType.OPT) {
            return;
        }
        dataService.optionPositionContractDetailsReceived(contractDetails);
    }
}
