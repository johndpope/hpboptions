package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.model.Chain;
import com.highpowerbear.hpboptions.model.ChainDataHolder;
import com.highpowerbear.hpboptions.model.ChainItem;
import com.ib.client.ContractDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by robertk on 12/21/2018.
 */
@Service
public class ChainService implements ConnectionListener {

    private final IbController ibController;
    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private final Map<Integer, Underlying> underlyingMap = new HashMap<>(); // conid -> underlying
    private final Map<Integer, SortedSet<String>> expirationsMap = new ConcurrentHashMap<>(); // underlying conid -> chain expirations
    private Chain activeChain;
    private final ReentrantLock chainLock = new ReentrantLock();

    private final Map<Integer, ChainDataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> chainDataHolder
    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL);

    @Autowired
    public ChainService(IbController ibController, CoreDao coreDao, MessageSender messageSender) {
        this.ibController = ibController;
        this.coreDao = coreDao;
        this.messageSender = messageSender;

        ibController.addConnectionListener(this);
        coreDao.getActiveUnderlyings().forEach(u -> underlyingMap.put(u.getConid(), u));
    }

    @Override
    public void postConnect() {
        cancelActiveChainMktData();
        requestActiveChainMktData();
    }

    @Override
    public void preDisconnect() {
        cancelActiveChainMktData();
    }

    @Scheduled(cron="0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        requestExpirations();
    }

    public boolean loadChain(int underlyingConid, String expiration) {
        chainLock.lock();
        try {
            if (!activeChain.isLoaded()) {
                return false; // complete the current task first
            }
            //TODO cancel current chain mkt data and clear activeChainMap

            activeChain = new Chain(underlyingConid, underlyingMap.get(underlyingConid).getSymbol(), LocalDate.parse(expiration, CoreSettings.IB_DATE_FORMATTER));
            // TODO request contract details

            return underlyingMap.containsKey(underlyingConid) && expirationsMap.get(underlyingConid).contains(expiration);

        } finally {
            chainLock.unlock();
        }
    }

    private void requestActiveChainMktData() {
        for (ChainItem item : activeChain.getItems()) {
            ChainDataHolder call = item.getCall();
            ChainDataHolder put = item.getPut();

            mktDataRequestMap.put(call.getIbMktDataRequestId(), call);
            mktDataRequestMap.put(put.getIbMktDataRequestId(), call);

            ibController.requestMktData(call.getIbMktDataRequestId(), call.toIbContract(), call.getGenericTicks());
            ibController.requestMktData(put.getIbMktDataRequestId(), put.toIbContract(), put.getGenericTicks());
        }
    }

    private void cancelActiveChainMktData() {
        mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
        mktDataRequestMap.clear();
    }

    private void requestExpirations() {
        underlyingMap.values().forEach(u -> ibController.requestOptionChainParams(ibRequestIdGen.incrementAndGet(), u.getSymbol(), u.getSecType(), u.getConid()));
    }

    private void requestActiveChainContractDetails() {
        // TODO
    }

    public void expirationsReceived(int underlyingConId, String exchange, int multiplier, Set<String> expirations) {
        Underlying underlying = underlyingMap.get(underlyingConId);

        if (Exchange.valueOf(exchange) == underlying.getChainExchange() && multiplier == underlying.getChainMultiplier()) {
            expirationsMap.put(underlyingConId, new TreeSet<>(expirations));
        }
        // TODO send expirations reload message
    }

    public void contractDetailsReceived(ContractDetails contractDetails) {
        // TODO
    }

    public void contractDetailsEndReceived(int requestId) {
        // TODO send chain reload request message
    }

    public Set<String> getExpirations(int underlyingConid) {
        return expirationsMap.get(underlyingConid);
    }

    public Chain getActiveChain() {
        return activeChain;
    }
}
