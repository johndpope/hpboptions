package com.highpowerbear.hpboptions.ibclient;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.ib.client.EClientSocket;
import com.ib.client.EJavaSignal;
import com.ib.client.EReaderSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class IbController {

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
}
