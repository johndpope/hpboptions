package com.highpowerbear.hpboptions.logic;

import com.ib.client.Contract;
import com.ib.client.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by robertk on 11/8/2018.
 */
@Service
public class OpenOrderHandler {
    private static final Logger log = LoggerFactory.getLogger(OpenOrderHandler.class);

    private final CoreDao coreDao;
    private final HeartbeatMonitor heartbeatMonitor;

    @Autowired
    public OpenOrderHandler(CoreDao coreDao, HeartbeatMonitor heartbeatMonitor) {
        this.coreDao = coreDao;
        this.heartbeatMonitor = heartbeatMonitor;
    }

    public void handleOpenOrder(int orderId, Contract contract, Order order) {
        // TODO
    }
}
