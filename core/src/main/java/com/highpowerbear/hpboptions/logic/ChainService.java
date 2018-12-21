package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.model.ChainDataHolder;
import com.ib.client.ContractDetails;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 12/21/2018.
 */
@Service
public class ChainService implements ConnectionListener {

    private final IbController ibController;
    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private final Map<Integer, Set<String>> chainExpirationsMap = new ConcurrentHashMap<>(); // underlying conid -> chain expirations
    private final SortedMap<Double, Pair<ChainDataHolder, ChainDataHolder>> activeChainMap = new TreeMap<>(); // strike -> (call, put)
    private int activeChainUnderlyingConid;

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL);

    @Autowired
    public ChainService(IbController ibController, CoreDao coreDao, MessageSender messageSender) {
        this.ibController = ibController;
        this.coreDao = coreDao;
        this.messageSender = messageSender;

        ibController.addConnectionListener(this);
    }

    @PostConstruct
    public void init() {
        // TODO
    }

    @Override
    public void postConnect() {
        // TODO
    }

    @Override
    public void preDisconnect() {
        // TODO
    }

    @Scheduled(cron="0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        // TODO update chain expirations
    }

    public String setActiveChainUnderlying(int underlyingConid) {
//        Instrument instr = underlyingMap.get(underlyingConid).getInstrument();
//
//        if (!chainExpirationsMap.containsKey(underlyingConid)) {
//            ibController.requestOptionChainParams(ibRequestIdGen.incrementAndGet(), instr.getSymbol(), instr.getSecType(), instr.getConid());
//        }
//        // TODO unsubscribe current chain data and clear activeChainMap send chain reload request message
//        activeChainUnderlyingConid = underlyingConid;
//
//        return instr.getSymbol();
        return "";
    }

    public void chainExpirationsReceived(int underlyingConId, Set<String> expirations) {
        chainExpirationsMap.put(underlyingConId, new TreeSet<>(expirations));
    }

    public void contractDetailsReceived(ContractDetails contractDetails) {
        // TODO
    }

    public Set<String> getActiveChainExpirations() {
        return chainExpirationsMap.get(activeChainUnderlyingConid);
    }

    public Collection<Pair<ChainDataHolder, ChainDataHolder>> getActiveChain() {
        return activeChainMap.values();
    }

}
