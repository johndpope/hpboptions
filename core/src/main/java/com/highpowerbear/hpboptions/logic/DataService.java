package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class DataService {

    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency

    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new LinkedHashMap<>(); // conid -> underlyingDataHolder
    private final Map<Integer, Map<Integer, PositionDataHolder>> underlyingPositionMap = new ConcurrentHashMap<>(); // underlying conid -> (position conid -> positionDataHolder)
    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder
    private final ReentrantLock positionLock = new ReentrantLock();

    private final Map<Integer, DataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> dataHolder
    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final Map<Integer, PositionDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> positionDataHolder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger();

    @Autowired
    public DataService(CoreDao coreDao, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        List<Underlying> underlyings = coreDao.getActiveUnderlyings();

        for (Underlying underlying : underlyings) {
            int conid = underlying.getConid();

            Instrument instrument = new Instrument(conid, underlying.getSecType(), underlying.getSymbol(), underlying.getCurrency());
            instrument.setExchange(underlying.getExchange());
            instrument.setPrimaryExchange(underlying.getPrimaryExchange());

            UnderlyingDataHolder underlyingDataHolder = new UnderlyingDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());

            underlyingMap.put(conid, underlyingDataHolder);
            underlyingPositionMap.put(conid, new ConcurrentHashMap<>());
        }
    }

    public void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    public void connect() {
        ibController.connect();

        if (isConnected()) {
            cancelAllMktData();

            underlyingMap.values().forEach(this::requestMktData);
            underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
            positionMap.values().forEach(this::requestMktData);
            ibController.requestPositions();
        }
    }

    public void disconnect() {
        cancelAllMktData();
        ibController.cancelPositions();
        CoreUtil.waitMilliseconds(1000);

        ibController.disconnect();
    }

    public boolean isConnected() {
        return ibController.isConnected();
    }

    public String getConnectionInfo() {
        return ibController.getIbConnectionInfo();
    }

    @Scheduled(fixedRate = 5000)
    private void reconnect() {
        if (!ibController.isConnected() && ibController.isMarkConnected()) {
            connect();
        }
    }

    @Scheduled(cron="0 0 6 * * MON-FRI")
    private void performStartOfDayTasks() {
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
        sendWsReloadRequestMessage(DataHolderType.POSITION);
    }

    private void requestMktData(DataHolder dataHolder) {
        int requestId = dataHolder.getIbMktDataRequestId();

        mktDataRequestMap.put(requestId, dataHolder);
        ibController.requestMktData(requestId, dataHolder.toIbContract(), dataHolder.getGenericTicks());
    }

    private void cancelMktData(DataHolder dataHolder) {
        int requestId = dataHolder.getIbMktDataRequestId();

        ibController.cancelMktData(requestId);
        mktDataRequestMap.remove(requestId);
    }

    private void cancelAllMktData() {
        mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
        mktDataRequestMap.clear();
    }

    private void requestImpliedVolatilityHistory(UnderlyingDataHolder underlyingDataHolder) {
        histDataRequestMap.putIfAbsent(underlyingDataHolder.getIbHistDataRequestId(), underlyingDataHolder);

        ibController.requestHistData(
                underlyingDataHolder.getIbHistDataRequestId(),
                underlyingDataHolder.toIbContract(),
                LocalDate.now().atStartOfDay().format(CoreSettings.IB_DATETIME_FORMATTER),
                IbDurationUnit.YEAR_1.getValue(),
                IbBarSize.DAY_1.getValue(),
                IbHistDataType.OPTION_IMPLIED_VOLATILITY.name(),
                IbTradingHours.REGULAR.getValue());
    }

    private void requestPnlSingle(PositionDataHolder positionDataHolder) {
        int requestId = positionDataHolder.getIbPnlRequestId();

        pnlRequestMap.put(requestId, positionDataHolder);
        ibController.requestPnlSingle(requestId, positionDataHolder.getInstrument().getConid());
    }

    private void cancelPnlSingle(PositionDataHolder positionDataHolder) {
        int requestId = positionDataHolder.getIbPnlRequestId();

        ibController.cancelPnlSingle(requestId);
        pnlRequestMap.remove(requestId);
    }

    private void sendWsMessage(DataHolder dataHolder, DataField field) {
        if (dataHolder.isSendMessage(field)) {
            messageSender.sendWsMessage(dataHolder.getType(), dataHolder.createMessage(field));
        }
    }

    private void sendWsReloadRequestMessage(DataHolderType type) {
        messageSender.sendWsMessage(type, "reload request");
    }

    private void recalculatePortfolioOptionData(int underlyingConid) {
        UnderlyingDataHolder underlyingDataHolder = underlyingMap.get(underlyingConid);
        if (!underlyingDataHolder.isCumulativeOptionDataUpdateDue()) { // throttling
            return;
        }
        Collection<PositionDataHolder> positionDataHolders = underlyingPositionMap.get(underlyingConid).values();
        if (positionDataHolders.stream().anyMatch(p -> !p.portfolioSourceFieldsReady())) {
            return;
        }
        double delta = 0d, gamma = 0d, vega = 0d, theta = 0d, timeValue = 0d;

        for (PositionDataHolder p : positionDataHolders) {
            int multiplier = p.getInstrument().getMultiplier();

            delta += p.getDelta() * p.getPositionSize() * multiplier;
            gamma += p.getGamma() * p.getPositionSize() * multiplier;
            vega += p.getVega() * p.getPositionSize() * multiplier;
            theta += p.getTheta() * p.getPositionSize() * multiplier;
            timeValue += p.getTimeValue() * Math.abs(p.getPositionSize()) * multiplier;
        }
        double lastPrice = underlyingDataHolder.getLast();
        double deltaDollars = CoreUtil.isValidPrice(lastPrice) ? delta * underlyingDataHolder.getLast() : Double.NaN;

        underlyingDataHolder.updatePortfolioOptionData(delta, gamma, vega, theta, timeValue, deltaDollars);
        UnderlyingDataField.portfolioFields().forEach(field -> sendWsMessage(underlyingDataHolder, field));
    }

    private void recalculateCumulativePnl(int underlyingConid) {
        UnderlyingDataHolder underlyingDataHolder = underlyingMap.get(underlyingConid);

        double unrealizedPnl = underlyingPositionMap.get(underlyingConid).values().stream().mapToDouble(PositionDataHolder::getUnrealizedPnl).sum();

        underlyingDataHolder.updateCumulativePnl(unrealizedPnl);
        sendWsMessage(underlyingDataHolder, UnderlyingDataField.UNREALIZED_PNL);
    }

    public void updateMktData(int requestId, int tickType, Number value) {
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        BasicMktDataField basicField = BasicMktDataField.basicField(TickType.get(tickType));
        if (basicField == null) {
            return;
        }
        dataHolder.updateField(basicField, value);
        DerivedMktDataField.derivedFields(basicField).forEach(dataHolder::calculateField);

        sendWsMessage(dataHolder, basicField);
        DerivedMktDataField.derivedFields(basicField).forEach(derivedField -> sendWsMessage(dataHolder, derivedField));
    }

    public void updateOptionData(int requestId, TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        if (tickType != TickType.BID_OPTION && tickType != TickType.ASK_OPTION && tickType != TickType.MODEL_OPTION) {
            return;
        }
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        OptionDataHolder optionDataHolder = (OptionDataHolder) dataHolder;
        optionDataHolder.updateOptionData(tickType, delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);
        OptionDataField.fields().forEach(field -> sendWsMessage(dataHolder, field));

        if (dataHolder.getType() == DataHolderType.POSITION) {
            recalculatePortfolioOptionData(optionDataHolder.getInstrument().getUnderlyingConid());
        }
    }

    public void historicalDataReceived(int requestId, Bar bar) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);

        LocalDate date = LocalDate.parse(bar.time(), CoreSettings.IB_DATE_FORMATTER);
        double value = bar.close();
        underlyingDataHolder.addImpliedVolatility(date, value);
    }

    public void historicalDataEnd(int requestId) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);
        underlyingDataHolder.impliedVolatilityHistoryCompleted();

        underlyingDataHolder.getIvHistoryDependentFields().forEach(field -> sendWsMessage(underlyingDataHolder, field));
    }

    public void optionPositionChanged(Contract contract, int positionSize) {
        positionLock.lock();
        try {
            int conid = contract.conid();
            PositionDataHolder positionDataHolder = positionMap.get(conid);

            if (positionDataHolder == null) {
                if (positionSize != 0) {
                    Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
                    String underlyingSymbol = contract.symbol();
                    String symbol = contract.localSymbol();
                    Currency currency = Currency.valueOf(contract.currency());
                    int multiplier = Integer.valueOf(contract.multiplier());

                    Types.Right right = contract.right();
                    double strike = contract.strike();
                    LocalDate expirationDate = LocalDate.parse(contract.lastTradeDateOrContractMonth(), CoreSettings.IB_DATE_FORMATTER);

                    OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expirationDate, multiplier, underlyingSymbol);
                    positionDataHolder = new PositionDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
                    positionDataHolder.updatePositionSize(positionSize);
                    positionMap.put(conid, positionDataHolder);

                    ibController.requestContractDetails(ibRequestIdGen.incrementAndGet(), contract);
                }
            } else if (positionSize != 0) {
                if (positionSize != positionDataHolder.getPositionSize()) {
                    positionDataHolder.updatePositionSize(positionSize);
                    sendWsMessage(positionDataHolder, PositionDataField.POSITION_SIZE);

                    recalculatePortfolioOptionData(positionDataHolder.getInstrument().getUnderlyingConid());
                }
            } else {
                cancelMktData(positionDataHolder);
                cancelPnlSingle(positionDataHolder);

                int underlyingConid = positionDataHolder.getInstrument().getUnderlyingConid();
                positionMap.remove(conid);
                underlyingPositionMap.get(underlyingConid).remove(conid);

                sendWsReloadRequestMessage(DataHolderType.POSITION);
            }
        } finally {
            positionLock.unlock();
        }
    }

    public void optionPositionContractDetailsReceived(ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();

        int conid = contract.conid();
        PositionDataHolder positionDataHolder = positionMap.get(conid);

        Exchange exchange = Exchange.valueOf(contract.exchange());
        double minTick = contractDetails.minTick();
        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        OptionInstrument instrument = positionDataHolder.getInstrument();
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        underlyingPositionMap.get(underlyingConid).put(conid, positionDataHolder);

        sendWsReloadRequestMessage(DataHolderType.POSITION);
        requestMktData(positionDataHolder);
        requestPnlSingle(positionDataHolder);
    }

    public void updatePositionUnrealizedPnl(int requestId, double unrealizedPnL) {
        PositionDataHolder positionDataHolder = pnlRequestMap.get(requestId);

        positionDataHolder.updateUnrealizedPnl(unrealizedPnL);
        sendWsMessage(positionDataHolder, PositionDataField.UNREALIZED_PNL);

        recalculateCumulativePnl(positionDataHolder.getInstrument().getUnderlyingConid());
    }

    public Collection<UnderlyingDataHolder> getUnderlyingDataHolders() {
        return underlyingMap.values();
    }

    public List<PositionDataHolder> getSortedPositionDataHolders() {

        return positionMap.values().stream()
                .sorted(Comparator
                        .comparing(PositionDataHolder::getDaysToExpiration)
                        .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                        .thenComparing(PositionDataHolder::getRight)
                        .thenComparingDouble(PositionDataHolder::getStrike)).collect(Collectors.toList());
    }

    public List<ChainDataHolder> getChainDataHolders() {
        return new ArrayList<>();
    }
}
