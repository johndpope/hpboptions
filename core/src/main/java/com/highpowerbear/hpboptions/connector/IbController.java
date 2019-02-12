package com.highpowerbear.hpboptions.connector;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.model.HopOrder;
import com.highpowerbear.hpboptions.model.IbConnectionInfo;
import com.highpowerbear.hpboptions.model.Instrument;
import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbController {
    private static final Logger log = LoggerFactory.getLogger(IbController.class);

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private final IbConnectionInfo ibConnectionInfo = new IbConnectionInfo();

    private EReaderSignal eReaderSignal;
    private EClientSocket eClientSocket;
    private boolean markConnected;

    private final List<ConnectionListener> connectionListeners = new ArrayList<>();

    void initialize(IbListener ibListener) {
        if (!initialized.get()) {
            initialized.set(true);

            eReaderSignal = new EJavaSignal();
            eClientSocket = new EClientSocket(ibListener, eReaderSignal);
        }
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public void connect() {
        connectionListeners.forEach(ConnectionListener::preConnect);

        markConnected = true;
        if (!isConnected()) {
            log.info("connecting " + getIbConnectionInfo());
            eClientSocket.eConnect(ibConnectionInfo.getHost(), ibConnectionInfo.getPort(), ibConnectionInfo.getClientId());
            HopUtil.waitMilliseconds(1000);

            if (isConnected()) {
                log.info("successfully connected " + getIbConnectionInfo());

                final EReader eReader = new EReader(eClientSocket, eReaderSignal);

                eReader.start();
                // an additional thread is created in this program design to empty the messaging queue
                new Thread(() -> {
                    while (eClientSocket.isConnected()) {
                        eReaderSignal.waitForSignal();
                        try {
                            eReader.processMsgs();
                        } catch (Exception e) {
                            log.error("error", e);
                        }
                    }
                }).start();
                connectionListeners.forEach(ConnectionListener::postConnect);
            }
        }
    }

    public void disconnect() {
        connectionListeners.forEach(ConnectionListener::preDisconnect);

        markConnected = false;
        if (isConnected()) {
            log.info("disconnecting " + getIbConnectionInfo());
            eClientSocket.eDisconnect();
            HopUtil.waitMilliseconds(1000);

            if (!isConnected()) {
                log.info("successfully disconnected " + getIbConnectionInfo());
                connectionListeners.forEach(ConnectionListener::postDisconnect);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    private void reconnect() {
        if (!isConnected() && markConnected) {
            connect();
        }
    }

    public Boolean isConnected() {
        return eClientSocket != null && eClientSocket.isConnected();
    }

    public void requestAccountSummary(int requestId, String tags) {
        log.info("requesting account summary, requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.reqAccountSummary(requestId, HopSettings.IB_ALL_ACCOUNTS_STRING, tags);
        }
    }

    public void cancelAccountSummary(int requestId) {
        log.info("canceling account summary for requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.cancelAccountSummary(requestId);
        }
    }

    public void requestMktData(int requestId, Contract contract, String genericTicks) {
        log.info("requesting market data for requestId=" + requestId + ", conid=" + contract.conid() + ", symbol=" + contract.localSymbol() + ", genericTicks=" + genericTicks);

        if (checkConnected()) {
            eClientSocket.reqMktData(requestId, contract, genericTicks, false, false, null);
        }
    }

    public void cancelMktData(int requestId) {
        log.info("canceling realtime data for requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.cancelMktData(requestId);
        }
    }

    public void requestHistData(int requestId, Contract contract, String endDateTime, String durationString, String barSizeSetting, String whatToShow, int useRTH) {
        log.info("requesting historical data, requestId=" + requestId + ", conid=" + contract.conid() + ", symbol=" + contract.localSymbol() +
                ", endDateTime=" + endDateTime + ", durationString=" + durationString + ", barSizeSetting=" + barSizeSetting +
                ", whatToShow=" + whatToShow + ", useRTH=" + useRTH);

        if (checkConnected()) {
            eClientSocket.reqHistoricalData(requestId, contract, endDateTime, durationString, barSizeSetting, whatToShow, useRTH, 1, false, null);
        }
    }

    public void requestPositions() {
        log.info("requesting positions");

        if (checkConnected()) {
            eClientSocket.reqPositions();
        }
    }

    public void cancelPositions() {
        log.info("canceling positions");

        if (checkConnected()) {
            eClientSocket.cancelPositions();
        }
    }

    public void requestOptionChainParams(int requestId, String underlyingSymbol, Types.SecType underlyingSecType, int underlyingConId) {
        log.info("requesting option chain parameters for requestId=" + requestId + ", underlying=" + underlyingSymbol);

        if (checkConnected()) {
            eClientSocket.reqSecDefOptParams(requestId, underlyingSymbol, "", underlyingSecType.name(), underlyingConId);
        }
    }

    public void requestContractDetails(int requestId, Contract contract) {
        log.info("requesting contract details for requestId=" + requestId + ", underlying=" + contract.symbol() + ", symbol=" + contract.localSymbol() + ", expiration=" + contract.lastTradeDateOrContractMonth());

        if (checkConnected()) {
            eClientSocket.reqContractDetails(requestId, contract);
        }
    }

    public void requestPnlSingle(int requestId, String ibAccount, int conid) {
        log.info("requesting pnl single for requestId=" + requestId + ", account=" + ibAccount + ", conId=" + conid);

        if (checkConnected()) {
            eClientSocket.reqPnLSingle(requestId, ibAccount, "", conid);
        }
    }

    public void cancelPnlSingle(int requestId) {
        log.info("canceling pnl single for requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.cancelPnLSingle(requestId);
        }
    }

    public void requestPnl(int requestId, String ibAccount) {
        log.info("requesting pnl for requestId=" + requestId + ", account=" + ibAccount);

        if (checkConnected()) {
            eClientSocket.reqPnL(requestId, ibAccount, "");
        }
    }

    public void cancelPnl(int requestId) {
        log.info("canceling pnl for requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.cancelPnL(requestId);
        }
    }

    public void requestOpenOrders() {
        log.info("requesting open orders");

        if (checkConnected()) {
            eClientSocket.reqOpenOrders();
        }
    }

    public void placeOrder(HopOrder hopOrder, Instrument instrument) {
        log.info("placing order " + hopOrder + ", instrument=" + instrument);

        if (checkConnected()) {
            eClientSocket.placeOrder(hopOrder.getOrderId(), instrument.createIbContract(), hopOrder.createIbOrder());
        }
    }

    public void cancelOrder(int orderId) {
        log.info("canceling order " + orderId);

        if (checkConnected()) {
            eClientSocket.cancelOrder(orderId);
        }
    }

    public IbConnectionInfo getIbConnectionInfo() {
        ibConnectionInfo.setConnected(isConnected());
        return ibConnectionInfo;
    }

    void connectionBroken() {
        eClientSocket.eDisconnect();
    }

    private boolean checkConnected() {
        if (!isConnected()) {
            log.info("not connected " + getIbConnectionInfo());
            return false;
        }
        return true;
    }
}
