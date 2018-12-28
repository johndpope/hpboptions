package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by robertk on 12/21/2018.
 */
@Service
public class ChainService extends AbstractService implements ConnectionListener {

    private final Map<Integer, Underlying> underlyingMap = new HashMap<>(); // conid -> underlying
    private final Map<Integer, SortedSet<LocalDate>> expirationsMap = new ConcurrentHashMap<>(); // underlying conid -> chain expirations

    private final Map<ChainInfo, SortedMap<Double, ChainItem>> chainMap = new HashMap<>(); // chainInfo -> (strike -> chainItem)
    private final Map<ChainInfo, List<ChainDataHolder>> chainDataHolderMap = new HashMap<>(); // chainInfo -> list of chain data holders
    private final Map<Integer, Integer> pendingChainRequests = new ConcurrentHashMap<>(); // ib request id -> underlying conid
    private final AtomicBoolean chainsRebuilt = new AtomicBoolean(false);
    private ChainInfo activeChainInfo;
    private final ReentrantLock chainLock = new ReentrantLock();

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL);

    @Autowired
    public ChainService(IbController ibController, CoreDao coreDao, MessageSender messageSender) {
        super(ibController, coreDao, messageSender);

        ibController.addConnectionListener(this);
        coreDao.getActiveUnderlyings().forEach(u -> underlyingMap.put(u.getConid(), u));
    }

    @Override
    public void postConnect() {
        if (!chainsRebuilt.get()) {
            rebuildChains();
        }
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
    }

    @Scheduled(cron="0 0 7 * * MON")
    private void performStartOfWeekTasks() {
        rebuildChains();
    }

    public ChainInfo loadChain(int underlyingConid, LocalDate expiration) {
        chainLock.lock();
        try {
            ChainInfo chainInfo = chainInfo(underlyingConid, expiration);
            if (chainInfo == null) {
                return null;
            }
            cancelAllMktData();
            activeChainInfo = chainInfo;
            chainDataHolderMap.get(activeChainInfo).forEach(this::requestMktData);
            return chainInfo;

        } finally {
            chainLock.unlock();
        }
    }

    private ChainInfo chainInfo(int underlyingConid, LocalDate expiration) {
        Underlying underlying = underlyingMap.get(underlyingConid);

        if (underlying != null && expirationsMap.get(underlyingConid).contains(expiration)) {
            return new ChainInfo(underlyingConid, underlying.getSymbol(), expiration);
        } else {
            return null;
        }
    }

    private void rebuildChains() {
        chainsRebuilt.set(false);

        expirationsMap.clear();
        chainMap.clear();
        chainDataHolderMap.clear();
        pendingChainRequests.clear();

        for (Underlying underlying : underlyingMap.values()) {
            int underlyingConid = underlying.getConid();
            String underlyingSymbol = underlying.getSymbol();
            Types.SecType underlyingSecType = underlying.getSecType();

            int expirationsRequestId = ibRequestIdGen.incrementAndGet();
            int contractDetailsRequestId = ibRequestIdGen.incrementAndGet();

            pendingChainRequests.put(expirationsRequestId, underlyingConid);
            pendingChainRequests.put(contractDetailsRequestId, underlyingConid);

            ibController.requestOptionChainParams(expirationsRequestId, underlyingSymbol, underlyingSecType, underlyingConid);
            // TODO request chain contract details
        }
    }

    public void expirationsReceived(int underlyingConId, String exchange, int multiplier, Set<String> expirationsStringSet) {
        Underlying underlying = underlyingMap.get(underlyingConId);

        if (Exchange.valueOf(exchange) == underlying.getChainExchange() && multiplier == underlying.getChainMultiplier()) {
            SortedSet<LocalDate> expirations = new TreeSet<>();

            expirationsStringSet.forEach(expiration -> expirations.add(LocalDate.parse(expiration, CoreSettings.IB_DATE_FORMATTER)));
            expirationsMap.put(underlyingConId, expirations);
        }
    }

    public void contractDetailsReceived(ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();

        int conid = contract.conid();
        Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
        String underlyingSymbol = contract.symbol();
        String symbol = contract.localSymbol();
        Currency currency = Currency.valueOf(contract.currency());
        int multiplier = Integer.valueOf(contract.multiplier());
        Exchange exchange = Exchange.valueOf(contract.exchange());

        double minTick = contractDetails.minTick();
        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        Types.Right right = contract.right();
        double strike = contract.strike();
        LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), CoreSettings.IB_DATE_FORMATTER);

        OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expiration, multiplier, underlyingSymbol);
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        ChainDataHolder chainDataHolder = new ChainDataHolder(instrument, ibRequestIdGen.incrementAndGet());
        ChainInfo chainInfo = chainInfo(underlyingConid, expiration);

        chainMap.putIfAbsent(chainInfo, new TreeMap<>());
        chainMap.get(chainInfo).putIfAbsent(strike, new ChainItem(strike));
        chainDataHolderMap.putIfAbsent(chainInfo, new ArrayList<>());

        chainMap.get(chainInfo).get(strike).setDataHolder(chainDataHolder);
        chainDataHolderMap.get(chainInfo).add(chainDataHolder);
    }

    public void chainsDataEndReceived(int requestId) {
        pendingChainRequests.remove(requestId);

        if (pendingChainRequests.isEmpty()) {
            chainsRebuilt.set(true);
            // TODO send expirations reload message
        }
    }

    public Set<LocalDate> getExpirations(int underlyingConid) {
        return expirationsMap.get(underlyingConid);
    }

    public Collection<ChainItem> getActiveChainItems() {
        return activeChainInfo != null ? chainMap.get(activeChainInfo).values() : new ArrayList<>();
    }
}
