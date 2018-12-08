package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.logic.CoreService;
import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbController {
    private static final Logger log = LoggerFactory.getLogger(IbController.class);

    private final String host = CoreSettings.IB_HOST;
    private final Integer port = CoreSettings.IB_PORT;
    private final Integer clientId = CoreSettings.IB_CLIENT_ID;

    private EReaderSignal eReaderSignal;
    private EClientSocket eClientSocket;
    private boolean markConnected;

    private final IbListener ibListener;
    private final CoreService coreService;

    @Autowired
    public IbController(IbListener ibListener, CoreService coreService) {
        this.ibListener = ibListener;
        this.coreService = coreService;
    }

    public String getIbConnectionInfo() {
        return host + ":" + port + ":" + clientId + "," + isConnected();
    }

    @PostConstruct
    public void init() {
        coreService.setIbController(this);
        ibListener.setIbController(this);
        ibListener.setCoreService(coreService);

        eReaderSignal = new EJavaSignal();
        eClientSocket = new EClientSocket(ibListener, eReaderSignal);
    }

    public void connect() {
        markConnected = true;

        if (!isConnected()) {
            log.info("connecting " + getIbConnectionInfo());
            eClientSocket.eConnect(host, port, clientId);
            CoreUtil.waitMilliseconds(1000);

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
            }
        }
    }

    public void disconnect() {
        markConnected = false;

        if (isConnected()) {
            log.info("disconnecting " + getIbConnectionInfo());
            eClientSocket.eDisconnect();
            CoreUtil.waitMilliseconds(1000);

            if (!isConnected()) {
                log.info("successfully disconnected " + getIbConnectionInfo());
            }
        }
    }

    public Boolean isConnected() {
        return eClientSocket != null && eClientSocket.isConnected();
    }

    public boolean isMarkConnected() {
        return markConnected;
    }

    public void requestMktData(int requestId, Contract contract, String genericTicks) {
        log.info("requesting market data, requestId=" + requestId + ", contract=" + CoreUtil.contractDetails(contract) + ", genericTicks=" + genericTicks);

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
        log.info("requesting historical data, requestId=" + requestId + ", contract=" + CoreUtil.contractDetails(contract) +
                ", endDateTime=" + endDateTime + ", durationString=" + durationString + ", barSizeSetting=" + barSizeSetting +
                ", whatToShow=" + whatToShow + ", useRTH=" + useRTH);

        if (checkConnected()) {
            eClientSocket.reqHistoricalData(requestId, contract, endDateTime, durationString, barSizeSetting, whatToShow, useRTH, 1, false, null);
        }
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
