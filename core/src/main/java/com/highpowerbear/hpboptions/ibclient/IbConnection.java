package com.highpowerbear.hpboptions.ibclient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.ib.client.EClientSocket;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by robertk on 11/5/2018.
 */
public class IbConnection {
    private static final Logger log = LoggerFactory.getLogger(IbConnection.class);

    private final String host;
    private final Integer port;
    private final Integer clientId;
    private String accounts; // csv, filled upon connection to IB, main account + FA subaccounts if any
    private boolean markConnected;
    @JsonIgnore
    private final EClientSocket eClientSocket; // null means not connected yet or manually disconnected
    @JsonIgnore
    private final EReaderSignal eReaderSignal;

    public IbConnection(String host, Integer port, Integer clientId, EClientSocket eClientSocket, EReaderSignal eReaderSignal) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.eClientSocket = eClientSocket;
        this.eReaderSignal = eReaderSignal;
    }

    public String getInfo() {
        return host + ":" + port + ":" + clientId + "," + isConnected();
    }

    public void connect() {
        markConnected = true;

        if (!isConnected()) {
            log.info("connecting " + getInfo());
            eClientSocket.eConnect(host, port, clientId);
            CoreUtil.waitMilliseconds(1000);

            if (isConnected()) {
                log.info("successfully connected " + getInfo());

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
            log.info("disconnecting " + getInfo());
            eClientSocket.eDisconnect();
            CoreUtil.waitMilliseconds(1000);

            if (!isConnected()) {
                log.info("successfully disconnected " + getInfo());
                this.accounts = null;
            }
        }
    }

    public boolean checkConnected() {
        if (!isConnected()) {
            log.info("not connected " + getInfo());
        }
        return isConnected();
    }

    @JsonProperty
    public Boolean isConnected() {
        return eClientSocket != null && eClientSocket.isConnected();
    }

    public void setAccounts(String accounts) {
        this.accounts = accounts;
    }

    public String getAccounts() {
        return accounts;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getClientId() {
        return clientId;
    }

    public boolean isMarkConnected() {
        return markConnected;
    }

    public EClientSocket getClientSocket() {
        return eClientSocket;
    }
}
