package com.highpowerbear.hpboptions.process;

import com.highpowerbear.hpboptions.dao.ProcessDao;
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

    private final ProcessDao processDao;
    private final HeartbeatControl heartbeatControl;

    @Autowired
    public OpenOrderHandler(ProcessDao processDao, HeartbeatControl heartbeatControl) {
        this.processDao = processDao;
        this.heartbeatControl = heartbeatControl;
    }

    public void handleOpenOrder(int orderId, Contract contract, Order order) {
        // TODO
    }
}
