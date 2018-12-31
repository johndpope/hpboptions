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
import com.highpowerbear.hpboptions.model.ChainItem;
import com.highpowerbear.hpboptions.model.OptionInstrument;
import com.highpowerbear.hpboptions.model.UnderlyingInfo;
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
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Map<Integer, Underlying> underlyingMap = new HashMap<>(); // conid -> underlying
    private final List<UnderlyingInfo> underlyingInfos = new ArrayList<>();
    private final Map<Integer, UnderlyingMktData> underlyingMktDataMap = new HashMap<>(); // conid -> subset of mkt data

    private final Map<Integer, SortedSet<LocalDate>> expirationsMap = new ConcurrentHashMap<>(); // underlying conid -> chain expirations
    private final Map<ChainKey, SortedMap<Double, ChainItem>> chainMap = new HashMap<>(); // chainInfo -> (strike -> chainItem)
    private ChainKey activeChainKey;
    private final Map<ChainKey, List<ChainDataHolder>> chainDataHolderMap = new HashMap<>(); // chainInfo -> list of chain data holders

    private final Map<Integer, Integer> pendingExpirationsRequests = new ConcurrentHashMap<>(); // ib request id -> underlying conid
    private final Map<Integer, Integer> pendingContractDetailsRequests = new ConcurrentHashMap<>(); // ib request id -> underlying conid
    private final AtomicBoolean chainsRebuilt = new AtomicBoolean(false);
    private final ReentrantLock chainLock = new ReentrantLock();

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_CHAIN_REQUEST_ID_INITIAL);

    private class UnderlyingMktData {
        private double price;
        private double optionImpliedVol;

        private UnderlyingMktData(double price, double optionImpliedVol) {
            this.price = price;
            this.optionImpliedVol = optionImpliedVol;
        }
    }

    private class ChainKey {
        private final int underlyingConid;
        private final LocalDate expiration;

        private ChainKey(int underlyingConid, LocalDate expiration) {
            this.underlyingConid = underlyingConid;
            this.expiration = expiration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChainKey chainKey = (ChainKey) o;

            if (underlyingConid != chainKey.underlyingConid) return false;
            return expiration.equals(chainKey.expiration);
        }

        @Override
        public int hashCode() {
            int result = underlyingConid;
            result = 31 * result + expiration.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return underlyingConid + "_" + expiration;
        }
    }

    @Autowired
    public ChainService(IbController ibController, CoreDao coreDao, MessageSender messageSender, RiskService riskService) {
        super(ibController, coreDao, messageSender);
        this.riskService = riskService;

        ibController.addConnectionListener(this);
        for (Underlying u : coreDao.getActiveUnderlyings()) {
            underlyingMap.put(u.getConid(), u);
            underlyingInfos.add(new UnderlyingInfo(u.getConid(), u.getSymbol()));
            expirationsMap.put(u.getConid(), new TreeSet<>());
        }
    }

    @Override
    public void postConnect() {
        if (!chainsRebuilt.get()) {
            executor.schedule(this::rebuildChains, CoreSettings.CHAIN_UNDERLYING_DATA_DELAY_MILLIS, TimeUnit.MILLISECONDS);
        } else if (activeChainKey != null) {
            requestActiveChainMktData();
        }
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
    }

    @Scheduled(cron="0 0 5 * * MON")
    private void performStartOfWeekTasks() {
        executor.execute(this::rebuildChains);
    }

    public boolean activateChain(int underlyingConid, LocalDate expiration) {
        chainLock.lock();
        try {
            if (!chainsRebuilt.get()) {
                return false;
            }
            ChainKey chainKey = chainKey(underlyingConid, expiration);
            if (chainKey == null) {
                return false;
            }
            activeChainKey = chainKey;
            requestActiveChainMktData();
            return true;

        } finally {
            chainLock.unlock();
        }
    }

    private void rebuildChains() {
        chainLock.lock();
        try {
            log.info("rebuilding chains");
            chainsRebuilt.set(false);
            if (!ibController.isConnected()) {
                return; // rebuild upon next connect
            }
            cancelAllMktData();
            activeChainKey = null;
            underlyingMktDataMap.clear();
            expirationsMap.values().forEach(Set::clear);
            chainMap.clear();
            chainDataHolderMap.clear();
            pendingExpirationsRequests.clear();
            pendingContractDetailsRequests.clear();

            for (Underlying underlying : underlyingMap.values()) {
                int underlyingConid = underlying.getConid();

                int expirationsRequestId = ibRequestIdGen.incrementAndGet();
                int contractDetailsRequestId = ibRequestIdGen.incrementAndGet();

                double underlyingPrice = riskService.getUnderlyingPrice(underlyingConid);
                double underlyingOptionImpliedVol = riskService.getUnderlyingOptionImpliedVol(underlyingConid);

                if (CoreUtil.isValidPrice(underlyingPrice) && CoreUtil.isValidPrice(underlyingOptionImpliedVol)) {
                    underlyingMktDataMap.put(underlyingConid, new UnderlyingMktData(underlyingPrice, underlyingOptionImpliedVol));
                }

                pendingExpirationsRequests.put(expirationsRequestId, underlyingConid);
                pendingContractDetailsRequests.put(contractDetailsRequestId, underlyingConid);

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
        log.info("requestActiveChainMktData for chainKey=" + activeChainKey);
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

        if (underlying.getChainExchange().name().equals(exchange) && multiplier == underlying.getChainMultiplier()) {
            expirationsStringSet.forEach(expiration -> expirationsMap.get(underlyingConId).add(LocalDate.parse(expiration, CoreSettings.IB_DATE_FORMATTER)));
        }
    }

    @Override
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
            ChainKey chainKey = chainKey(underlyingConid, expiration);

            chainMap.putIfAbsent(chainKey, new TreeMap<>());
            chainMap.get(chainKey).putIfAbsent(strike, new ChainItem(strike));
            chainDataHolderMap.putIfAbsent(chainKey, new ArrayList<>());

            chainMap.get(chainKey).get(strike).setupDataHolder(chainDataHolder);
            chainDataHolderMap.get(chainKey).add(chainDataHolder);
        }
    }

    public void expirationsEndReceived(int requestId) {
        pendingExpirationsRequests.remove(requestId);
        log.info("expirationsEndReceived, requestId=" + requestId + ", remaining=" + pendingExpirationsRequests.size());
        checkChainsRebuilt();
    }

    @Override
    public void contractDetailsEndReceived(int requestId) {
        pendingContractDetailsRequests.remove(requestId);
        log.info("contractDetailsEndReceived, requestId=" + requestId + ", remaining=" + pendingContractDetailsRequests.size());
        checkChainsRebuilt();
    }

    private void checkChainsRebuilt() {
        if (pendingExpirationsRequests.isEmpty() && pendingContractDetailsRequests.isEmpty()) {
            chainsRebuilt.set(true);
            log.info("chains rebuilt");
            chainMap.keySet().forEach(key -> log.info(key + " -> " + chainMap.get(key).size() + " strikes"));
            messageSender.sendWsReloadRequestMessage(DataHolderType.CHAIN);
        }
    }

    public List<UnderlyingInfo> getUnderlyingInfos() {
        return underlyingInfos;
    }

    public Set<LocalDate> getExpirations(int underlyingConid) {
        return expirationsMap.get(underlyingConid);
    }

    public Collection<ChainItem> getActiveChainItems() {
        return activeChainKey != null ? chainMap.get(activeChainKey).values() : new ArrayList<>();
    }
}
