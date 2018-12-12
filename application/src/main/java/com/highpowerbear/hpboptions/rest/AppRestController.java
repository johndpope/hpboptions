package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.logic.DataService;
import com.highpowerbear.hpboptions.model.PositionDataHolder;
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

    private final DataService dataService;

    @Autowired
    public AppRestController(DataService dataService) {
        this.dataService = dataService;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connect")
    public ResponseEntity<?> connect() {
        if (!dataService.isConnected()) {
            dataService.connect();
        }

        return ResponseEntity.ok("connected=" + dataService.isConnected());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "disconnect")
    public ResponseEntity<?> disconnect() {
        if (dataService.isConnected()) {
            dataService.disconnect();
            CoreUtil.waitMilliseconds(1000);
        }
        return ResponseEntity.ok("connected=" + dataService.isConnected());
    }

    @RequestMapping("connection-info")
    public ResponseEntity<?> getConnectionInfo() {
        return ResponseEntity.ok(dataService.getConnectionInfo());
    }

    @RequestMapping("underlying-data-holders")
    public ResponseEntity<?> getUnderlyingDataHolders() {
        List<UnderlyingDataHolder> underlyingDataHolders = dataService.getUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, (long) underlyingDataHolders.size()));
    }

    @RequestMapping("position-data-holders")
    public ResponseEntity<?> getPositionDataHolders() {
        List<PositionDataHolder> positionDataHolders = dataService.getPositionDataHolders();
        return ResponseEntity.ok(new RestList<>(positionDataHolders, (long) positionDataHolders.size()));
    }
}