package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.logic.ChainService;
import com.highpowerbear.hpboptions.logic.DataService;
import com.highpowerbear.hpboptions.logic.OrderService;
import com.highpowerbear.hpboptions.model.PositionDataHolder;
import com.highpowerbear.hpboptions.model.UnderlyingDataHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by robertk on 11/22/2018.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final IbController ibController;
    private final DataService dataService;
    private final OrderService orderService;
    private final ChainService chainService;

    @Autowired
    public AppRestController(IbController ibController, DataService dataService, OrderService orderService, ChainService chainService) {
        this.ibController = ibController;
        this.dataService = dataService;
        this.orderService = orderService;
        this.chainService = chainService;
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

        Collection<UnderlyingDataHolder> underlyingDataHolders = dataService.getSortedUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, underlyingDataHolders.size()));
    }

    @RequestMapping("position-data-holders")
    public ResponseEntity<?> getPositionDataHolders() {

        List<PositionDataHolder> positionDataHolders = dataService.getSortedPositionDataHolders();
        return ResponseEntity.ok(new RestList<>(positionDataHolders, positionDataHolders.size()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "chain-underlying/{conid}")
    public ResponseEntity<?> setActiveChainUnderlying(@PathVariable("conid") int conid) {

        String underlyingSymbol = chainService.setActiveChainUnderlying(conid);
        return ResponseEntity.ok(underlyingSymbol);
    }

    @RequestMapping("active-chain-expirations")
    public ResponseEntity<?> getActiveChainExpirations() {

        Set<String> expirations = chainService.getActiveChainExpirations();
        return ResponseEntity.ok(new RestList<>(expirations, expirations.size()));
    }
}