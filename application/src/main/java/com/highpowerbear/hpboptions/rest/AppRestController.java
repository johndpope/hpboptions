package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.corelogic.ConnectionController;
import com.highpowerbear.hpboptions.corelogic.DataController;
import com.highpowerbear.hpboptions.corelogic.model.Underlying;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by robertk on 11/22/2018.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final ConnectionController connectionController;
    private final DataController dataController;

    @Autowired
    public AppRestController(ConnectionController connectionController, DataController dataController) {
        this.connectionController = connectionController;
        this.dataController = dataController;
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

    @RequestMapping("underlyings")
    public ResponseEntity<?> getUnderlyings() {
        List<Underlying> underlyings = dataController.getUnderlyings();
        return ResponseEntity.ok(new RestList<>(underlyings, (long) underlyings.size()));
    }
}