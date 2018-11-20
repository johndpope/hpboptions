package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReaderSignal;
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

    private final IbListener ibListener;
    private IbConnection ibConnection;

    public IbConnection getIbConnection() {
        return ibConnection;
    }

    @Autowired
    public IbController(IbListener ibListener) {
        this.ibListener = ibListener;
    }

    @PostConstruct
    private void init() {
        EReaderSignal eReaderSignal = new EJavaSignal();
        EClientSocket eClientSocket = new EClientSocket(ibListener, eReaderSignal);

        ibConnection = new IbConnection(CoreSettings.IB_HOST, CoreSettings.IB_PORT, CoreSettings.IB_CLIENT_ID, eClientSocket, eReaderSignal);
    }

    public void connect() {
        ibConnection.connect();
    }

    public void disconnect() {
        ibConnection.disconnect();
    }

    public boolean requestRealtimeData(int reqId, Contract contract) {
        log.info("requesting realtime data, reqId=" + reqId + ", contract=" + CoreUtil.printIbContract(contract));

        if (ibConnection.checkConnected()) {
            ibConnection.getClientSocket().reqMktData(reqId, contract, "", false, null);
            return true;
        }
        return false;
    }

    public boolean cancelRealtimeData(int reqId) {
        log.info("canceling realtime data for reqId=" + reqId);

        if (ibConnection.checkConnected()) {
            ibConnection.getClientSocket().cancelMktData(reqId);
            return true;
        }
        return false;
    }
}
