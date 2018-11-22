package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.ibclient.IbConnection;
import com.highpowerbear.hpboptions.ibclient.IbController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Created by robertk on 11/22/2018.
 */
public class CoreScheduler {

    private final CoreDao coreDao;
    private final IbController ibController;
    private final HeartbeatMonitor heartbeatMonitor;

    @Autowired
    public CoreScheduler(CoreDao coreDao, IbController ibController, HeartbeatMonitor heartbeatMonitor) {
        this.coreDao = coreDao;
        this.ibController = ibController;
        this.heartbeatMonitor = heartbeatMonitor;
    }

    @Scheduled(fixedRate = 5000)
    private void reconnect() {
        IbConnection c = ibController.getIbConnection();
        if (!c.isConnected() && c.isMarkConnected()) {
            c.connect();
        }
    }
}
