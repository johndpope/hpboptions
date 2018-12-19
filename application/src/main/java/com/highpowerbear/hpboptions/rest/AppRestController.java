package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.logic.DataService;
import com.highpowerbear.hpboptions.model.PositionDataHolder;
import com.highpowerbear.hpboptions.model.UnderlyingDataHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * Created by robertk on 11/22/2018.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final IbController ibController;
    private final DataService dataService;

    @Autowired
    public AppRestController(IbController ibController, DataService dataService) {
        this.ibController = ibController;
        this.dataService = dataService;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connect")
    public ResponseEntity<?> connect() {
        if (!ibController.isConnected()) {
            ibController.connect();
        }

        return ResponseEntity.ok("connected=" + ibController.isConnected());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "disconnect")
    public ResponseEntity<?> disconnect() {
        if (ibController.isConnected()) {
            ibController.disconnect();
            CoreUtil.waitMilliseconds(1000);
        }
        return ResponseEntity.ok("connected=" + ibController.isConnected());
    }

    @RequestMapping("account-summary")
    public ResponseEntity<?> getAccountSummary() {
        return ResponseEntity.ok(dataService.getAccountSummaryText());
    }

    @RequestMapping("connection-info")
    public ResponseEntity<?> getConnectionInfo() {
        return ResponseEntity.ok(ibController.getConnectionInfo());
    }

    @RequestMapping("underlying-data-holders")
    public ResponseEntity<?> getUnderlyingDataHolders() {
        Collection<UnderlyingDataHolder> underlyingDataHolders = dataService.getUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, (long) underlyingDataHolders.size()));
    }

    @RequestMapping("position-data-holders")
    public ResponseEntity<?> getPositionDataHolders() {
        List<PositionDataHolder> positionDataHolders = dataService.getSortedPositionDataHolders();
        return ResponseEntity.ok(new RestList<>(positionDataHolders, (long) positionDataHolders.size()));
    }
}