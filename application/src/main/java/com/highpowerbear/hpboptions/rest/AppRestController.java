package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.logic.CoreService;
import com.highpowerbear.hpboptions.model.UnderlyingDataHolder;
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

    private final CoreService coreService;

    @Autowired
    public AppRestController(CoreService coreService) {
        this.coreService = coreService;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connect")
    public ResponseEntity<?> connect() {
        if (!coreService.isConnected()) {
            coreService.connect();
        }

        return ResponseEntity.ok("connected=" + coreService.isConnected());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "disconnect")
    public ResponseEntity<?> disconnect() {
        if (coreService.isConnected()) {
            coreService.disconnect();
            CoreUtil.waitMilliseconds(1000);
        }
        return ResponseEntity.ok("connected=" + coreService.isConnected());
    }

    @RequestMapping("connection-info")
    public ResponseEntity<?> getConnectionInfo() {
        return ResponseEntity.ok(coreService.getConnectionInfo());
    }

    @RequestMapping("underlying-data-holders")
    public ResponseEntity<?> getUnderlyingDataHolders() {
        List<UnderlyingDataHolder> underlyingDataHolders = coreService.getUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, (long) underlyingDataHolders.size()));
    }
}