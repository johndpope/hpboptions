package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.dataholder.OptionDataHolder;
import com.highpowerbear.hpboptions.field.PositionDataField;
import com.highpowerbear.hpboptions.model.OptionInstrument;
import com.highpowerbear.hpboptions.dataholder.PositionDataHolder;
import com.highpowerbear.hpboptions.dataholder.ActiveUnderlyingDataHolder;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by robertk on 1/23/2019.
 */
@Service
public class PositionService extends AbstractMarketDataService {

    private final ActiveUnderlyingService activeUnderlyingService;

    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder
    private final Map<Integer, PositionDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> positionDataHolder
    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.POSITION_IB_REQUEST_ID_INITIAL);

    private final ReentrantLock positionLock = new ReentrantLock();
    private PositionSortOrder positionSortOrder = PositionSortOrder.EXPIRATION;

    @Value("${ib.account}")
    private String ibAccount;

    public PositionService(IbController ibController, MessageService messageService, ActiveUnderlyingService activeUnderlyingService) {
        super(ibController, messageService);
        this.activeUnderlyingService = activeUnderlyingService;

        ibController.addConnectionListener(this);
    }

    @Override
    public void postConnect() {
        cancelAllMktData();
        cancelAllPnlSingle();

        positionMap.values().forEach(this::requestMktData);
        positionMap.values().forEach(this::requestPnlSingle);
        ibController.requestPositions();
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
        cancelAllPnlSingle();
        ibController.cancelPositions();
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        ibController.requestPositions();
    }

    private void requestPnlSingle(PositionDataHolder pdh) {
        int requestId = pdh.getIbPnlRequestId();

        pnlRequestMap.put(requestId, pdh);
        ibController.requestPnlSingle(requestId, ibAccount, pdh.getInstrument().getConid());
    }

    private void cancelPnlSingle(PositionDataHolder pdh) {
        int requestId = pdh.getIbPnlRequestId();

        ibController.cancelPnlSingle(requestId);
        pnlRequestMap.remove(requestId);
    }

    private void cancelAllPnlSingle() {
        pnlRequestMap.keySet().forEach(ibController::cancelPnlSingle);
        pnlRequestMap.clear();
    }

    @Override
    public void modelOptionDataReceived(OptionDataHolder optionDataHolder) {
        PositionDataHolder pdh = (PositionDataHolder) optionDataHolder;
        pdh.calculateMargin();
        messageService.sendWsMessage(pdh, PositionDataField.MARGIN);

        activeUnderlyingService.calculateRiskDataPerUnderlying(pdh.getInstrument().getUnderlyingConid());
    }

    public void positionReceived(Contract contract, int positionSize) {
        positionLock.lock();
        try {
            int conid = contract.conid();
            PositionDataHolder pdh = positionMap.get(conid);

            if (pdh == null) {
                if (positionSize != 0) {
                    Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
                    String symbol = contract.localSymbol();
                    String underlyingSymbol = contract.symbol();
                    Currency currency = Currency.valueOf(contract.currency());
                    Types.Right right = contract.right();
                    double strike = contract.strike();
                    LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), HopSettings.IB_DATE_FORMATTER);
                    double multiplier = Double.valueOf(contract.multiplier());

                    OptionInstrument instrument = new OptionInstrument(conid, secType, underlyingSymbol, symbol, currency, right, strike, expiration);
                    instrument.setMultiplier(multiplier);
                    pdh = new PositionDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());

                    if (pdh.getDaysToExpiration() >= 0) {
                        pdh.updatePositionSize(positionSize);
                        positionMap.put(conid, pdh);
                        ibController.requestContractDetails(ibRequestIdGen.incrementAndGet(), contract);
                    }
                }
            } else {
                if (positionSize != 0 && pdh.getDaysToExpiration() >= 0) {
                    if (positionSize != pdh.getPositionSize()) {
                        pdh.updatePositionSize(positionSize);
                        messageService.sendWsMessage(pdh, PositionDataField.POSITION_SIZE);

                        int underlyingConid = pdh.getInstrument().getUnderlyingConid();
                        activeUnderlyingService.calculateOptionPositionsSum(underlyingConid);
                        activeUnderlyingService.calculateRiskDataPerUnderlying(underlyingConid);
                    }
                } else {
                    cancelMktData(pdh);
                    cancelPnlSingle(pdh);

                    positionMap.remove(conid);
                    int underlyingConid = pdh.getInstrument().getUnderlyingConid();

                    activeUnderlyingService.removeOptionPosition(underlyingConid, conid);
                    activeUnderlyingService.calculateRiskDataPerUnderlying(underlyingConid);
                    activeUnderlyingService.calculateUnrealizedPnlPerUnderlying(underlyingConid);

                    messageService.sendWsReloadRequestMessage(DataHolderType.POSITION);
                }
            }

        } finally {
            positionLock.unlock();
        }
    }

    @Override
    public void contractDetailsReceived(int requestId, ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();

        int conid = contract.conid();
        PositionDataHolder pdh = positionMap.get(conid);

        Exchange exchange = Exchange.valueOf(contract.exchange());
        double minTick = contractDetails.minTick();
        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        OptionInstrument instrument = pdh.getInstrument();
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        ActiveUnderlyingDataHolder udh = activeUnderlyingService.getUnderlyingDataHolder(underlyingConid);
        if (udh != null) {
            pdh.setDisplayRank(udh.getDisplayRank());
            activeUnderlyingService.addOptionPosition(underlyingConid, pdh);
        } else {
            pdh.setDisplayRank(-1);
        }

        messageService.sendWsReloadRequestMessage(DataHolderType.POSITION);
        requestMktData(pdh);
        requestPnlSingle(pdh);
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        PositionDataHolder pdh = pnlRequestMap.get(requestId);

        if (pdh != null) {
            pdh.updateUnrealizedPnl(unrealizedPnL);
            messageService.sendWsMessage(pdh, PositionDataField.UNREALIZED_PNL);
            activeUnderlyingService.calculateUnrealizedPnlPerUnderlying(pdh.getInstrument().getUnderlyingConid());
        }
    }

    public PositionDataHolder getPositionDataHolder(int conid) {
        return positionMap.get(conid);
    }

    public PositionSortOrder getPositionSortOrder() {
        return positionSortOrder;
    }

    public void setPositionSortOrder(PositionSortOrder positionSortOrder) {
        this.positionSortOrder = positionSortOrder;
    }

    public List<PositionDataHolder> getSortedPositionDataHolders() {
        if (positionSortOrder == PositionSortOrder.EXPIRATION) {
            return positionMap.values().stream()
                    .sorted(Comparator
                            .comparing(PositionDataHolder::getDaysToExpiration)
                            .thenComparing(PositionDataHolder::getDisplayRank)
                            .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                            .thenComparing(PositionDataHolder::getRight)
                            .thenComparingDouble(PositionDataHolder::getStrike)).collect(Collectors.toList());
        } else {
            return positionMap.values().stream()
                    .sorted(Comparator
                            .comparing(PositionDataHolder::getDisplayRank)
                            .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                            .thenComparing(PositionDataHolder::getDaysToExpiration)
                            .thenComparing(PositionDataHolder::getRight)
                            .thenComparingDouble(PositionDataHolder::getStrike)).collect(Collectors.toList());
        }
    }
}
