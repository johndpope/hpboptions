package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.corelogic.HeartbeatMonitor;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.ibclient.IbController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by robertk on 11/22/2018.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final CoreDao processDao;
    private final IbController ibController;
    private final HeartbeatMonitor heartbeatMonitor;

    @Autowired
    public AppRestController(CoreDao processDao, IbController ibController, HeartbeatMonitor heartbeatMonitor) {
        this.processDao = processDao;
        this.ibController = ibController;
        this.heartbeatMonitor = heartbeatMonitor;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connect")
    public ResponseEntity<?> connect() {
        ibController.connect();
        CoreUtil.waitMilliseconds(1000);
        return ResponseEntity.ok("connected: " + ibController.isConnected());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "disconnect")
    public ResponseEntity<?> disconnect() {
        ibController.disconnect();
        CoreUtil.waitMilliseconds(1000);
        return ResponseEntity.ok("connected: " + ibController.isConnected());
    }
}