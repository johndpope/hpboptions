package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.ibclient.IbConnection;
import com.highpowerbear.hpboptions.ibclient.IbController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by robertk on 11/23/2018.
 */
@Component
public class ConnectionController {

    private final IbController ibController;
    private final RealtimeDataController realtimeDataController;

    @Autowired
    public ConnectionController(IbController ibController, RealtimeDataController realtimeDataController) {
        this.ibController = ibController;
        this.realtimeDataController = realtimeDataController;
    }

    public void connect() {
        ibController.getIbConnection().connect();
        realtimeDataController.requestAllUndlRealtimeData();
    }

    public void disconnect() {
        realtimeDataController.cancelAllUndlRealtimeData();
        CoreUtil.waitMilliseconds(1000);
        ibController.getIbConnection().disconnect();
    }

    public boolean isConnected() {
        return ibController.getIbConnection().isConnected();
    }

    @Scheduled(fixedRate = 5000)
    private void reconnect() {
        IbConnection c = ibController.getIbConnection();
        if (!c.isConnected() && c.isMarkConnected()) {
            connect();
        }
    }
}
