package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.database.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by robertk on 12/21/2018.
 */
@Service
public class ChainService extends AbstractDataService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(ChainService.class);

    private final RiskService riskService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    private final Map<Integer, Underlying> underlyingMap = new HashMap<>(); // conid -> underlying
    private final List<UnderlyingInfo> underlyingInfos = new ArrayList<>();
    private final Map<Integer, UnderlyingMktDataSnapshot> underlyingMktDataSnapshotMap = new HashMap<>(); // conid -> subset of mkt data

    private final Map<Integer, SortedSet<LocalDate>> expirationsMap = new HashMap<>(); // underlying conid -> chain expirations
    private final Map<ChainKey, SortedMap<Double, ChainItem>> chainMap = new HashMap<>(); // chainKey -> (strike -> chainItem)
    private ChainKey activeChainKey;
    private final Map<ChainKey, List<ChainDataHolder>> chainDataHolderMap = new HashMap<>(); // chainKey -> list of chain data holders
    private final Map<Integer, ChainDataHolder> conidMap = new HashMap<>(); // conid -> chainDataHolder

    private final Map<Integer, Integer> expirationsRequestMap = new ConcurrentHashMap<>(); // ib request id -> underlying conid
    private final Map<Integer, ChainKey> contractDetailsRequestMap = new ConcurrentHashMap<>(); // ib request id -> chainKey
    private final AtomicBoolean allChainsReady = new AtomicBoolean(false);
    private final ReentrantLock chainLock = new ReentrantLock();

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.CHAIN_IB_REQUEST_ID_INITIAL);

    private class UnderlyingMktDataSnapshot {
        private double price;
        private double optionImpliedVol;

        private UnderlyingMktDataSnapshot(double price, double optionImpliedVol) {
            this.price = price;
            this.optionImpliedVol = optionImpliedVol;
        }
    }

    @Autowired
    public ChainService(IbController ibController, HopDao hopDao, MessageService messageService, RiskService riskService) {
        super(ibController, hopDao, messageService);
        this.riskService = riskService;

        ibController.addConnectionListener(this);
        for (Underlying u : hopDao.getActiveUnderlyings()) {
            underlyingMap.put(u.getConid(), u);
            underlyingInfos.add(new UnderlyingInfo(u.getConid(), u.getSymbol()));
            expirationsMap.put(u.getConid(), new TreeSet<>());
        }
    }

    @Override
    public void postConnect() {
        if (!allChainsReady.get()) {
            expirationsRequestMap.clear();
            contractDetailsRequestMap.clear();
            executor.schedule(this::rebuildChains, HopSettings.CHAIN_REBUILD_DELAY_MILLIS, TimeUnit.MILLISECONDS);

        } else if (activeChainKey != null) {
            requestActiveChainMktData();
        }
    }

    @Override
    public void preDisconnect() {
        expirationsRequestMap.clear();
        contractDetailsRequestMap.clear();
        cancelAllMktData();
    }

    @Scheduled(cron="0 0 5 * * MON")
    private void performStartOfWeekTasks() {
        executor.execute(this::rebuildChains);
    }

    public ChainActivationResult activateChain(int underlyingConid, LocalDate expiration) {
        chainLock.lock();
        try {
            ChainKey chainKey = chainKey(underlyingConid, expiration);
            if (chainKey == null || chainMap.get(chainKey) == null) {
                return new ChainActivationResult(false, null, null, null);
            }
            if (activeChainKey == null || !activeChainKey.equals(chainKey)) {
                activeChainKey = chainKey;
                if (ibController.isConnected()) {
                    requestActiveChainMktData();
                }
            }
            return new ChainActivationResult(true, underlyingConid, underlyingMap.get(underlyingConid).getSymbol(), expiration);
        } finally {
            chainLock.unlock();
        }
    }

    private void rebuildChains() {
        chainLock.lock();
        try {
            if (!expirationsRequestMap.isEmpty() || !contractDetailsRequestMap.isEmpty()) {
                log.info("chains rebuilding already in progress");
                return;
            }
            log.info("rebuilding chains");
            allChainsReady.set(false);
            if (!ibController.isConnected()) {
                return; // rebuild upon next connect
            }
            cancelAllMktData();
            activeChainKey = null;
            underlyingMktDataSnapshotMap.clear();
            expirationsMap.values().forEach(Set::clear);
            chainMap.clear();
            chainDataHolderMap.clear();
            conidMap.clear();

            for (UnderlyingInfo underlyingInfo : underlyingInfos) {
                expirationsRequestMap.put(ibRequestIdGen.incrementAndGet(), underlyingInfo.getConid());

                double underlyingPrice = riskService.getUnderlyingPrice(underlyingInfo.getConid());
                double underlyingOptionImpliedVol = riskService.getUnderlyingOptionImpliedVol(underlyingInfo.getConid());

                if (HopUtil.isValidPrice(underlyingPrice) && HopUtil.isValidPrice(underlyingOptionImpliedVol)) {
                    underlyingMktDataSnapshotMap.put(underlyingInfo.getConid(), new UnderlyingMktDataSnapshot(underlyingPrice, underlyingOptionImpliedVol));
                }
            }

            for (int requestId : expirationsRequestMap.keySet()) {
                int underlyingConid = expirationsRequestMap.get(requestId);

                Underlying underlying = underlyingMap.get(underlyingConid);
                ibController.requestOptionChainParams(requestId, underlying.getSymbol(), underlying.getSecType(), underlyingConid);
            }

        } finally {
            chainLock.unlock();
        }
    }

    private void requestActiveChainMktData() {
        log.info("requesting active chain mkt data for chainKey=" + activeChainKey);
        cancelAllMktData();
        chainDataHolderMap.get(activeChainKey).forEach(this::requestMktData);
    }

    private ChainKey chainKey(int underlyingConid, LocalDate expiration) {
        Underlying underlying = underlyingMap.get(underlyingConid);

        return underlying != null && expirationsMap.get(underlyingConid).contains(expiration) ?
                new ChainKey(underlyingConid, expiration) :
                null;
    }

    private boolean isEligibleToAdd(ChainDataHolder cdh) {
        int underlyingConId = cdh.getInstrument().getUnderlyingConid();

        Underlying underlying = underlyingMap.get(underlyingConId);
        UnderlyingMktDataSnapshot snapshot = underlyingMktDataSnapshotMap.get(cdh.getInstrument().getUnderlyingConid());
        double strike = cdh.getInstrument().getStrike();

        if (snapshot == null || underlying.isChainRoundStrikes() && !HopUtil.isRound(strike)) {
            return false;
        }
        double priceStdDev = snapshot.price * snapshot.optionImpliedVol * Math.sqrt((double) cdh.getDaysToExpiration() / 365);
        double lowerStrike = snapshot.price - (priceStdDev * HopSettings.CHAIN_STRIKES_STD_DEVIATIONS);
        double upperStrike = snapshot.price + (priceStdDev * HopSettings.CHAIN_STRIKES_STD_DEVIATIONS);

        return strike >= lowerStrike && strike <= upperStrike;
    }

    public void expirationsReceived(int underlyingConId, String exchange, int multiplier, Set<String> expirationsStringSet) {
        Underlying underlying = underlyingMap.get(underlyingConId);
        LocalDate now = LocalDate.now();

        if (underlying.getChainExchange().name().equals(exchange) && multiplier == underlying.getChainMultiplier()) {
            expirationsStringSet.stream()
                    .map(exp -> LocalDate.parse(exp, HopSettings.IB_DATE_FORMATTER))
                    .filter(exp -> !now.isAfter(exp))
                    .forEach(exp -> expirationsMap.get(underlyingConId).add(exp));
        }
    }

    public void expirationsEndReceived(int requestId) {
        log.info("expirations received for underlyingConid=" + expirationsRequestMap.get(requestId) + ", request=" + requestId);
        expirationsRequestMap.remove(requestId);

        if (expirationsRequestMap.isEmpty()) {
            messageService.sendWsReloadRequestMessage(DataHolderType.CHAIN);
            requestContractDetails();
        }
    }

    private void requestContractDetails() {
        for (int underlyingConid : expirationsMap.keySet()) {
            for (LocalDate expiration : expirationsMap.get(underlyingConid)) {
                contractDetailsRequestMap.put(ibRequestIdGen.incrementAndGet(), chainKey(underlyingConid, expiration));
            }
        }
        // prioritize requests, earliest expirations first
        Set<Integer> prioritizedRequests = new LinkedHashSet<>();

        contractDetailsRequestMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator
                        .comparing(ChainKey::getExpiration)
                        .thenComparing(ChainKey::getUnderlyingConid)))
                .forEachOrdered(entry -> prioritizedRequests.add(entry.getKey()));

        // execute in a new thread
        executor.execute(() -> {
            for (int requestId : prioritizedRequests) {
                ChainKey chainKey = contractDetailsRequestMap.get(requestId);
                Underlying underlying = underlyingMap.get(chainKey.getUnderlyingConid());

                ibController.requestContractDetails(requestId, underlying.createChainRequestContract(chainKey.getExpiration()));
                HopUtil.waitMilliseconds(HopSettings.CHAIN_CONTRACT_DETAILS_REQUEST_WAIT_MILLIS);
            }
        });
    }

    @Override
    public void contractDetailsReceived(int requestId, ContractDetails contractDetails) {
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
        LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), HopSettings.IB_DATE_FORMATTER);

        OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expiration, multiplier, underlyingSymbol);
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        ChainDataHolder chainDataHolder = new ChainDataHolder(instrument, ibRequestIdGen.incrementAndGet());
        if (isEligibleToAdd(chainDataHolder)) {
            ChainKey chainKey = chainKey(underlyingConid, expiration);

            chainMap.putIfAbsent(chainKey, new TreeMap<>());
            chainMap.get(chainKey).putIfAbsent(strike, new ChainItem(strike));
            chainDataHolderMap.putIfAbsent(chainKey, new ArrayList<>());

            chainMap.get(chainKey).get(strike).setupDataHolder(chainDataHolder);
            chainDataHolderMap.get(chainKey).add(chainDataHolder);
            conidMap.put(conid, chainDataHolder);
        }
    }

    @Override
    public void contractDetailsEndReceived(int requestId) {
        log.info("contract details received for chainKey=" + contractDetailsRequestMap.get(requestId) + ", requestId=" + requestId);
        contractDetailsRequestMap.remove(requestId);

        if (contractDetailsRequestMap.isEmpty()) {
            allChainsReady.set(true);
            log.info("all chains ready");
        }
    }

    public ChainDataHolder getChainDataHolder(int conid) {
        return conidMap.get(conid);
    }

    public List<UnderlyingInfo> getUnderlyingInfos() {
        return underlyingInfos;
    }

    public Set<LocalDate> getExpirations(int underlyingConid) {
        return expirationsMap.get(underlyingConid);
    }

    public ChainKey getActiveChainKey() {
        return activeChainKey;
    }

    public Collection<ChainItem> getActiveChainItems() {
        return activeChainKey != null ? chainMap.get(activeChainKey).values() : new ArrayList<>();
    }
}
