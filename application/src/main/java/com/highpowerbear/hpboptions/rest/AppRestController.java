package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.enums.ChainActivationStatus;
import com.highpowerbear.hpboptions.service.ChainService;
import com.highpowerbear.hpboptions.service.RiskService;
import com.highpowerbear.hpboptions.service.OrderService;
import com.highpowerbear.hpboptions.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    private final RiskService riskService;
    private final OrderService orderService;
    private final ChainService chainService;

    @Autowired
    public AppRestController(IbController ibController, RiskService riskService, OrderService orderService, ChainService chainService) {
        this.ibController = ibController;
        this.riskService = riskService;
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
            HopUtil.waitMilliseconds(1000);
        }
        return ResponseEntity.ok("connected=" + ibController.isConnected());
    }

    @RequestMapping("account-summary")
    public ResponseEntity<?> getAccountSummary() {
        return ResponseEntity.ok(riskService.getAccountSummaryText());
    }

    @RequestMapping("connection-info")
    public ResponseEntity<?> getConnectionInfo() {
        return ResponseEntity.ok(ibController.getConnectionInfo());
    }

    @RequestMapping("underlying-data-holders")
    public ResponseEntity<?> getUnderlyingDataHolders() {

        List<UnderlyingDataHolder> underlyingDataHolders = riskService.getSortedUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, underlyingDataHolders.size()));
    }

    @RequestMapping("position-data-holders")
    public ResponseEntity<?> getPositionDataHolders() {

        List<PositionDataHolder> positionDataHolders = riskService.getSortedPositionDataHolders();
        return ResponseEntity.ok(new RestList<>(positionDataHolders, positionDataHolders.size()));
    }

    @RequestMapping("underlying-infos")
    public ResponseEntity<?> getUnderlyingInfos() {

        List<UnderlyingInfo> underlyingInfos = chainService.getUnderlyingInfos();
        return ResponseEntity.ok(new RestList<>(underlyingInfos, underlyingInfos.size()));
    }

    @RequestMapping("expirations/{underlyingConid}")
    public ResponseEntity<?> getExpirations(
            @PathVariable("underlyingConid") int underlyingConid) {

        Set<LocalDate> expirations = chainService.getExpirations(underlyingConid);
        return ResponseEntity.ok(new RestList<>(expirations, expirations.size()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "activate-chain/{underlyingConid}/{expiration}")
    public ResponseEntity<?> activateChain(
            @PathVariable("underlyingConid") int underlyingConid,
            @PathVariable("expiration") @DateTimeFormat(pattern = HopSettings.JSON_DATE_FORMAT) LocalDate expiration) {

        ChainActivationStatus chainActivationStatus = chainService.activateChain(underlyingConid, expiration);
        return ResponseEntity.ok(chainActivationStatus.name().toLowerCase());
    }

    @RequestMapping("active-chain-key")
    public ResponseEntity<?> getActiveChainKey() {
        ChainKey activeChainKey = chainService.getActiveChainKey();
        return activeChainKey != null ? ResponseEntity.ok(chainService.getActiveChainKey()) : ResponseEntity.ok("NA");
    }

    @RequestMapping("active-chain-items")
    public ResponseEntity<?> getActiveChainItems() {
        Collection<ChainItem> chainItems = chainService.getActiveChainItems();
        return ResponseEntity.ok(new RestList<>(chainItems, chainItems.size()));
    }
}