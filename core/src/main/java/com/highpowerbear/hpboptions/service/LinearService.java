package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.ib.client.Contract;
import org.springframework.stereotype.Service;

/**
 * Created by robertk on 5/28/2019.
 */
@Service
public class LinearService extends AbstractMarketDataService {

    private final HopDao hopDao;

    public LinearService(IbController ibController, MessageService messageService, HopDao hopDao) {
        super(ibController, messageService);
        this.hopDao = hopDao;

        ibController.addConnectionListener(this);
        init();
    }

    private void init() {
        // TODO
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        // TODO
    }

    public void positionReceived(Contract contract, int positionSize) {
        // TODO
    }
}
