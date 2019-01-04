package com.highpowerbear.hpboptions.connector;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
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

    private final String host = CoreSettings.IB_HOST;
    private final Integer port = CoreSettings.IB_PORT;
    private final Integer clientId = CoreSettings.IB_CLIENT_ID;

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
            log.info("connecting " + getConnectionInfo());
            eClientSocket.eConnect(host, port, clientId);
            CoreUtil.waitMilliseconds(1000);

            if (isConnected()) {
                log.info("successfully connected " + getConnectionInfo());

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
            log.info("disconnecting " + getConnectionInfo());
            eClientSocket.eDisconnect();
            CoreUtil.waitMilliseconds(1000);

            if (!isConnected()) {
                log.info("successfully disconnected " + getConnectionInfo());
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
            eClientSocket.reqAccountSummary(requestId, "All", tags);
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

    public void requestPnlSingle(int requestId, int conid) {
        log.info("requesting pnl single for requestId=" + requestId + ", conId=" + conid);

        if (checkConnected()) {
            eClientSocket.reqPnLSingle(requestId, "U1884205", "", conid);
        }
    }

    public void cancelPnlSingle(int requestId) {
        log.info("canceling pnl single for requestId=" + requestId);

        if (checkConnected()) {
            eClientSocket.cancelPnLSingle(requestId);
        }
    }

    public void requestOpenOrders() {
        log.info("requesting openOrders, allOpenOrders and autoOpenOrders");

        if (checkConnected()) {
            eClientSocket.reqOpenOrders();
            eClientSocket.reqAllOpenOrders();
            eClientSocket.reqAutoOpenOrders(true);
        }
    }

    public String getConnectionInfo() {
        return host + ":" + port + ":" + clientId + "," + isConnected();
    }

    void connectionBroken() {
        eClientSocket.eDisconnect();
    }

    private boolean checkConnected() {
        if (!isConnected()) {
            log.info("not connected " + getConnectionInfo());
            return false;
        }
        return true;
    }
}
