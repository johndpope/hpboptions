package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.corelogic.ConnectionController;
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

    private final ConnectionController connectionController;

    @Autowired
    public AppRestController(ConnectionController connectionController) {
        this.connectionController = connectionController;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connect")
    public ResponseEntity<?> connect() {
        connectionController.connect();

        return ResponseEntity.ok("connected: " + connectionController.isConnected());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "disconnect")
    public ResponseEntity<?> disconnect() {
        connectionController.disconnect();
        CoreUtil.waitMilliseconds(1000);
        return ResponseEntity.ok("connected: " + connectionController.isConnected());
    }
}