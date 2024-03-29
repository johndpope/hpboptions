package com.highpowerbear.hpboptions.rest;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.dataholder.*;
import com.highpowerbear.hpboptions.enums.PositionSortOrder;
import com.highpowerbear.hpboptions.model.*;
import com.highpowerbear.hpboptions.rest.model.CreateOrderParams;
import com.highpowerbear.hpboptions.rest.model.RestList;
import com.highpowerbear.hpboptions.rest.model.SendOrderParams;
import com.highpowerbear.hpboptions.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Created by robertk on 11/22/2018.
 */
@RestController
@RequestMapping("/")
public class AppRestController {

    private final IbController ibController;
    private final AccountService accountService;
    private final ActiveUnderlyingService activeUnderlyingService;
    private final LinearService linearService;
    private final OrderService orderService;
    private final PositionService positionService;
    private final ChainService chainService;
    private final ScannerService scannerService;

    @Autowired
    public AppRestController(IbController ibController, AccountService accountService, ActiveUnderlyingService activeUnderlyingService, LinearService linearService,
                             OrderService orderService, PositionService positionService, ChainService chainService, ScannerService scannerService) {
        this.ibController = ibController;
        this.accountService = accountService;
        this.activeUnderlyingService = activeUnderlyingService;
        this.linearService = linearService;
        this.orderService = orderService;
        this.positionService = positionService;
        this.chainService = chainService;
        this.scannerService = scannerService;
    }

    @RequestMapping("connection/info")
    public ResponseEntity<?> getConnectionInfo() {
        return ResponseEntity.ok(ibController.getIbConnectionInfo());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connection/connect")
    public ResponseEntity<?> connect() {
        if (!ibController.isConnected()) {
            ibController.connect();
        }
        return ResponseEntity.ok(ibController.getIbConnectionInfo());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "connection/disconnect")
    public ResponseEntity<?> disconnect() {
        if (ibController.isConnected()) {
            ibController.disconnect();
            HopUtil.waitMilliseconds(1000);
        }
        return ResponseEntity.ok(ibController.getIbConnectionInfo());
    }

    @RequestMapping("account/data-holders")
    public ResponseEntity<?> getAccountDataHolders() {
        Collection<AccountDataHolder> accountDataHolders = accountService.getAccountDataHolders();
        return ResponseEntity.ok(new RestList<>(accountDataHolders, accountDataHolders.size()));
    }

    @RequestMapping("underlying/data-holders")
    public ResponseEntity<?> getUnderlyingDataHolders() {
        List<ActiveUnderlyingDataHolder> underlyingDataHolders = activeUnderlyingService.getUnderlyingDataHolders();
        return ResponseEntity.ok(new RestList<>(underlyingDataHolders, underlyingDataHolders.size()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "underlying/{conid}/delta-hedge/{deltaHedge}")
    public ResponseEntity<?> setDeltaHedge(
            @PathVariable("conid") int conid,
            @PathVariable("deltaHedge") boolean deltaHedge) {

        activeUnderlyingService.setDeltaHedge(conid, deltaHedge);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("linear/data-holders")
    public ResponseEntity<?> getLinearDataHolders() {
        List<LinearDataHolder> linearDataHolders = linearService.getLinearDataHolders();
        return ResponseEntity.ok(new RestList<>(linearDataHolders, linearDataHolders.size()));
    }

    @RequestMapping("/position/sort-order")
    public ResponseEntity<?> getPositionSortOrder() {
        return ResponseEntity.ok(positionService.getPositionSortOrder().name().toLowerCase());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/position/sort-order/{sortOrder}")
    public ResponseEntity<?> setPositionSortOrder(
            @PathVariable("sortOrder") String sortOrder) {

        positionService.setPositionSortOrder(PositionSortOrder.valueOf(sortOrder.toUpperCase()));
        return ResponseEntity.ok().build();
    }

    @RequestMapping("position/data-holders")
    public ResponseEntity<?> getPositionDataHolders() {
        List<PositionDataHolder> positionDataHolders = positionService.getSortedPositionDataHolders();
        return ResponseEntity.ok(new RestList<>(positionDataHolders, positionDataHolders.size()));
    }

    @RequestMapping("/order/filter")
    public ResponseEntity<?> getOrderFilter() {
        return ResponseEntity.ok(orderService.getOrderFilter());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/order/filter")
    public ResponseEntity<?> setOrderFilter(
            @RequestBody OrderFilter orderFilter) {

        orderService.setOrderFilter(orderFilter);
        return ResponseEntity.ok().build();
    }

    @RequestMapping("order/data-holders")
    public ResponseEntity<?> getOrderDataHolders() {
        List<OrderDataHolder>orderDataHolders = orderService.getFilteredOrderDataHolders();
        return ResponseEntity.ok(new RestList<>(orderDataHolders, orderDataHolders.size()));
    }

    @RequestMapping(method = RequestMethod.POST, value = "order/create-from/underlying")
    public ResponseEntity<?> createOrderFromUnderlying(
            @RequestBody CreateOrderParams createOrderParams) {

        orderService.createOrderFromUnderlying(createOrderParams.getConid(), createOrderParams.getAction());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.POST, value = "order/create-from/linear")
    public ResponseEntity<?> createOrderFromLinear(
            @RequestBody CreateOrderParams createOrderParams) {

        orderService.createOrderFromLinear(createOrderParams.getConid(), createOrderParams.getAction());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.POST, value = "order/create-from/position")
    public ResponseEntity<?> createOrderFromPosition(
            @RequestBody CreateOrderParams createOrderParams) {

        orderService.createOrderFromPosition(createOrderParams.getConid(), createOrderParams.getAction());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.POST, value = "order/create-from/chain")
    public ResponseEntity<?> createOrderFromChain(
            @RequestBody CreateOrderParams createOrderParams) {

        orderService.createOrderFromChain(createOrderParams.getConid(), createOrderParams.getAction());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/order/{orderId}/send")
    public ResponseEntity<?> sendOrder(
            @PathVariable("orderId") int orderId,
            @RequestBody SendOrderParams p) {

        orderService.sendOrder(orderId, p.getQuantity(), p.getLimitPrice(), p.isAdapt());
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/order/send")
    public ResponseEntity<?> sendOrders(
            @RequestBody List<SendOrderParams> sendOrderParamsList) {

        sendOrderParamsList.stream()
                .sorted(Comparator.comparingInt(SendOrderParams::getOrderId))
                .forEach(p -> orderService.sendOrder(p.getOrderId(), p.getQuantity(), p.getLimitPrice(), p.isAdapt()));

        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/order/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable("orderId") int orderId) {

        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/order/remove-idle")
    public ResponseEntity<?> removeIdleOrders() {
        orderService.removeIdleOrders();
        return ResponseEntity.ok().build();
    }

    @RequestMapping("chain/underlying-infos")
    public ResponseEntity<?> getUnderlyingInfos() {
        List<UnderlyingInfo> underlyingInfos = activeUnderlyingService.getUnderlyingInfos();
        return ResponseEntity.ok(new RestList<>(underlyingInfos, underlyingInfos.size()));
    }

    @RequestMapping("chain/{underlyingConid}/expirations")
    public ResponseEntity<?> getExpirations(
            @PathVariable("underlyingConid") int underlyingConid) {

        Set<LocalDate> expirations = chainService.getExpirations(underlyingConid);
        return ResponseEntity.ok(new RestList<>(expirations, expirations.size()));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "chain/{underlyingConid}/activate/{expiration}")
    public ResponseEntity<?> activateChain(
            @PathVariable("underlyingConid") int underlyingConid,
            @PathVariable("expiration") @DateTimeFormat(pattern = HopSettings.JSON_DATE_FORMAT) LocalDate expiration) {

        boolean success = chainService.activateChain(underlyingConid, expiration);
        return ResponseEntity.ok(success);
    }

    @RequestMapping("chain/active-key")
    public ResponseEntity<?> getActiveChainKey() {
        ChainKey activeChainKey = chainService.getActiveChainKey();
        return activeChainKey != null ? ResponseEntity.ok(chainService.getActiveChainKey()) : ResponseEntity.ok("NA");
    }

    @RequestMapping("chain/active-items")
    public ResponseEntity<?> getActiveChainItems() {
        Collection<ChainItem> chainItems = chainService.getActiveChainItems();
        return ResponseEntity.ok(new RestList<>(chainItems, chainItems.size()));
    }
}
