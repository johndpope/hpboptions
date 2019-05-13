package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by robertk on 5/13/2019.
 */
@Service
public class ScannerService extends AbstractMarketDataService implements ConnectionListener {

    @Autowired
    public ScannerService(IbController ibController, MessageService messageService) {
        super(ibController, messageService);

        ibController.addConnectionListener(this);
    }
}
