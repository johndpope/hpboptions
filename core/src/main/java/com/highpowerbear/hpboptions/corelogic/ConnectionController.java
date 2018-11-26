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

    private final InstrumentRepo instrumentRepo;
    private final IbController ibController;
    private final DataController dataController;

    @Autowired
    public ConnectionController(InstrumentRepo instrumentRepo, IbController ibController, DataController dataController) {
        this.instrumentRepo = instrumentRepo;
        this.ibController = ibController;
        this.dataController = dataController;
    }

    public void connect() {
        instrumentRepo.refreshUndlInstruments();
        ibController.getIbConnection().connect();
        dataController.reset();
        instrumentRepo.getUndlInstruments().forEach(dataController::requestData);
    }

    public void disconnect() {
        instrumentRepo.getUndlInstruments().forEach(dataController::cancelData);
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
