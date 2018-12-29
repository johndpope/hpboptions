package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.model.ChainDataHolder;
import com.highpowerbear.hpboptions.model.ChainInfo;
import com.highpowerbear.hpboptions.model.ChainItem;
import com.highpowerbear.hpboptions.model.OptionInstrument;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
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

    private final DataService dataService;
    private final AsyncTaskExecutor asyncTaskExecutor;

    private final Map<Integer, Underlying> underlyingMap = new HashMap<>(); // conid -> underlying
    private final Map<Integer, UnderlyingMktData> underlyingMktDataMap = new HashMap<>(); // conid -> subset of mkt data

    private final Map<Integer, SortedSet<LocalDate>> expirationsMap = new ConcurrentHashMap<>(); // underlying conid -> chain expirations
    private final Map<ChainInfo, SortedMap<Double, ChainItem>> chainMap = new HashMap<>(); // chainInfo -> (strike -> chainItem)
    private final Map<ChainInfo, List<ChainDataHolder>> chainDataHolderMap = new HashMap<>(); // chainInfo -> list of chain data holders

    private final Map<Integer, Integer> pendingChainRequests = new ConcurrentHashMap<>(); // ib request id -> underlying conid
    private final AtomicBoolean chainsRebuilt = new AtomicBoolean(false);
    private ChainInfo activeChainInfo;
    private final ReentrantLock chainLock = new ReentrantLock();

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL);

    private class UnderlyingMktData {
        private double price;
        private double optionImpliedVol;

        public UnderlyingMktData(double price, double optionImpliedVol) {
            this.price = price;
            this.optionImpliedVol = optionImpliedVol;
        }
    }

    @Autowired
    public ChainService(IbController ibController, CoreDao coreDao, MessageSender messageSender, DataService dataService, AsyncTaskExecutor asyncTaskExecutor) {
        super(ibController, coreDao, messageSender);
        this.dataService = dataService;
        this.asyncTaskExecutor = asyncTaskExecutor;

        ibController.addConnectionListener(this);
        coreDao.getActiveUnderlyings().forEach(u -> underlyingMap.put(u.getConid(), u));
    }

    @Override
    public void postConnect() {
        if (!chainsRebuilt.get()) {
            asyncTaskExecutor.execute(this::rebuildChains, CoreSettings.CHAIN_UNDERLYING_DATA_WAIT_MILLIS);
        } else if (activeChainInfo != null) {
            requestActiveChainMktData();
        }
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
    }

    @Scheduled(cron="0 0 5 * * MON")
    private void performStartOfWeekTasks() {
        asyncTaskExecutor.execute(this::rebuildChains);
    }

    public ChainInfo loadChain(int underlyingConid, LocalDate expiration) {
        chainLock.lock();
        try {
            ChainInfo chainInfo = chainInfo(underlyingConid, expiration);
            if (chainInfo == null) {
                return null;
            }
            activeChainInfo = chainInfo;
            requestActiveChainMktData();
            return chainInfo;

        } finally {
            chainLock.unlock();
        }
    }

    private void rebuildChains() {
        chainLock.lock();
        try {
            chainsRebuilt.set(false);
            if (!ibController.isConnected()) {
                return; // rebuild upon next connect
            }
            cancelAllMktData();
            expirationsMap.clear();
            chainMap.clear();
            chainDataHolderMap.clear();
            pendingChainRequests.clear();

            for (Underlying underlying : underlyingMap.values()) {
                int underlyingConid = underlying.getConid();

                int expirationsRequestId = ibRequestIdGen.incrementAndGet();
                int contractDetailsRequestId = ibRequestIdGen.incrementAndGet();

                double underlyingPrice = dataService.getUnderlyingPrice(underlyingConid);
                double underlyingOptionImpliedVol = dataService.getUnderlyingOptionImpliedVol(underlyingConid);

                if (CoreUtil.isValidPrice(underlyingPrice) && CoreUtil.isValidPrice(underlyingOptionImpliedVol)) {
                    underlyingMktDataMap.put(underlyingConid, new UnderlyingMktData(underlyingPrice, underlyingOptionImpliedVol));
                } else {
                    underlyingMktDataMap.remove(underlyingConid);
                }

                pendingChainRequests.put(expirationsRequestId, underlyingConid);
                pendingChainRequests.put(contractDetailsRequestId, underlyingConid);

                ibController.requestOptionChainParams(expirationsRequestId, underlying.getSymbol(), underlying.getSecType(), underlyingConid);
                ibController.requestContractDetails(contractDetailsRequestId, underlying.createChainRequestContract());
            }
        } finally {
            chainLock.unlock();
        }
    }

    private void requestActiveChainMktData() {
        if (!ibController.isConnected()) {
            return;
        }
        cancelAllMktData();
        chainDataHolderMap.get(activeChainInfo).forEach(this::requestMktData);
    }

    private ChainInfo chainInfo(int underlyingConid, LocalDate expiration) {
        Underlying underlying = underlyingMap.get(underlyingConid);

        if (underlying != null && expirationsMap.get(underlyingConid).contains(expiration)) {
            return new ChainInfo(underlyingConid, underlying.getSymbol(), expiration);
        } else {
            return null;
        }
    }

    private boolean isEligibleToAdd(ChainDataHolder cdh) {
        int underlyingConId = cdh.getInstrument().getUnderlyingConid();

        Underlying underlying = underlyingMap.get(underlyingConId);
        UnderlyingMktData umd = underlyingMktDataMap.get(cdh.getInstrument().getUnderlyingConid());
        double strike = cdh.getInstrument().getStrike();

        if (umd == null || underlying.isChainRoundStrikes() && !CoreUtil.isRound(strike)) {
            return false;
        }
        double priceStdDev = umd.price * umd.optionImpliedVol * Math.sqrt((double) cdh.getDaysToExpiration() / 365);
        double lowerStrike = umd.price - (priceStdDev * CoreSettings.CHAIN_STRIKES_STD_DEVIATIONS);
        double upperStrike = umd.price + (priceStdDev * CoreSettings.CHAIN_STRIKES_STD_DEVIATIONS);

        return strike >= lowerStrike && strike <= upperStrike;
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
        if (isEligibleToAdd(chainDataHolder)) {
            ChainInfo chainInfo = chainInfo(underlyingConid, expiration);

            chainMap.putIfAbsent(chainInfo, new TreeMap<>());
            chainMap.get(chainInfo).putIfAbsent(strike, new ChainItem(strike));
            chainDataHolderMap.putIfAbsent(chainInfo, new ArrayList<>());

            chainMap.get(chainInfo).get(strike).setupDataHolder(chainDataHolder);
            chainDataHolderMap.get(chainInfo).add(chainDataHolder);
        }
    }

    public void chainsDataEndReceived(int requestId) {
        pendingChainRequests.remove(requestId);

        if (pendingChainRequests.isEmpty()) {
            chainsRebuilt.set(true);
            messageSender.sendWsReloadRequestMessage(DataHolderType.CHAIN);
        }
    }

    public Set<LocalDate> getExpirations(int underlyingConid) {
        return expirationsMap.get(underlyingConid);
    }

    public Collection<ChainItem> getActiveChainItems() {
        return activeChainInfo != null ? chainMap.get(activeChainInfo).values() : new ArrayList<>();
    }
}
