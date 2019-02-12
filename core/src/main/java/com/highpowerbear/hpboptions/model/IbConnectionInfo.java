package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.common.HopSettings;

/**
 * Created by robertk on 2/12/2019.
 */
public class IbConnectionInfo {

    private final String host = HopSettings.IB_HOST;
    private final int port = HopSettings.IB_PORT;
    private final int clientId = HopSettings.IB_CLIENT_ID;
    private boolean connected = false;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getClientId() {
        return clientId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public String toString() {
        return "host=" + host + ", port=" + port + ", clientId=" + clientId + ", connected=" + connected;
    }
}
