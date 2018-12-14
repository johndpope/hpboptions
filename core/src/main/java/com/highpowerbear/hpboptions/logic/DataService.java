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

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class DataService {

    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency

    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new HashMap<>(); // conid -> underlyingDataHolder
    private final List<UnderlyingDataHolder> underlyingDataHolders = new ArrayList<>();
    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder
    private final ReentrantLock positionLock = new ReentrantLock();

    private final Map<Integer, DataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> dataHolder
    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder

    private final AtomicInteger ibMktDataRequestIdGen = new AtomicInteger(0);
    private final AtomicInteger ibHistDataRequestIdGen = new AtomicInteger(1000000);
    private final AtomicInteger ibContractDetailsRequestIdGen = new AtomicInteger(2000000);

    @Autowired
    public DataService(CoreDao coreDao, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        List<Underlying> underlyings = coreDao.getActiveUnderlyings();

        for (Underlying underlying : underlyings) {
            UnderlyingDataHolder underlyingDataHolder = new UnderlyingDataHolder(
                    underlying.createInstrument(),
                    ibMktDataRequestIdGen.incrementAndGet(),
                    ibHistDataRequestIdGen.incrementAndGet());

            underlyingMap.put(underlying.getConid(), underlyingDataHolder);
            underlyingDataHolders.add(underlyingDataHolder);
        }
    }

    public void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    public void connect() {
        ibController.connect();

        if (isConnected()) {
            cancelAllMktData();

            underlyingDataHolders.forEach(this::requestMktData);
            underlyingDataHolders.forEach(this::requestImpliedVolatilityHistory);
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
        underlyingDataHolders.forEach(this::requestImpliedVolatilityHistory);
        sendReloadRequestMessage(DataHolderType.POSITION);
    }

    private void requestMktData(DataHolder dataHolder) {
        mktDataRequestMap.put(dataHolder.getIbMktDataRequestId(), dataHolder);
        ibController.requestMktData(dataHolder.getIbMktDataRequestId(), dataHolder.getInstrument().toIbContract(), dataHolder.getGenericTicks());
    }

    private void cancelMktData(DataHolder dataHolder) {
        ibController.cancelMktData(dataHolder.getIbMktDataRequestId());
        mktDataRequestMap.remove(dataHolder.getIbMktDataRequestId());
    }

    private void cancelAllMktData() {
        mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
        mktDataRequestMap.clear();
    }

    private void requestImpliedVolatilityHistory(UnderlyingDataHolder underlyingDataHolder) {
        histDataRequestMap.putIfAbsent(underlyingDataHolder.getIbHistDataRequestId(), underlyingDataHolder);

        ibController.requestHistData(
                underlyingDataHolder.getIbHistDataRequestId(),
                underlyingDataHolder.getInstrument().toIbContract(),
                LocalDate.now().atStartOfDay().format(CoreSettings.IB_DATETIME_FORMATTER),
                IbDurationUnit.YEAR_1.getValue(),
                IbBarSize.DAY_1.getValue(),
                IbHistDataType.OPTION_IMPLIED_VOLATILITY.name(),
                IbTradingHours.REGULAR.getValue());
    }

    private void sendReloadRequestMessage(DataHolderType type) {
        messageSender.sendWsMessage(type, "reload request");
    }

    private void recalculateCumulativeData(int underlyingConid) {
        UnderlyingDataHolder underlyingDataHolder = underlyingMap.get(underlyingConid);
        // TODO
    }

    public void updateMktData(int requestId, int tickType, Number value) {
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        BasicMktDataField basicField = BasicMktDataField.getBasicField(TickType.get(tickType));
        if (basicField == null) {
            return;
        }
        dataHolder.updateField(basicField, value);
        DerivedMktDataField.getDerivedFields(basicField).forEach(dataHolder::calculateField);

        if (dataHolder.isSendMessage(basicField)) {
            messageSender.sendWsMessage(dataHolder.getType(), dataHolder.createMessage(basicField));
        }

        DerivedMktDataField.getDerivedFields(basicField).stream()
                .filter(dataHolder::isSendMessage)
                .forEach(derivedField -> messageSender.sendWsMessage(dataHolder.getType(), dataHolder.createMessage(derivedField)));
    }

    public void updateOptionData(int requestId, int tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        ((OptionDataHolder) dataHolder).updateOptionData(TickType.get(tickType), delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);

        OptionDataField.getValues().stream()
                .filter(dataHolder::isSendMessage)
                .forEach(field -> messageSender.sendWsMessage(dataHolder.getType(), dataHolder.createMessage(field)));

        if (dataHolder.getType() == DataHolderType.POSITION) {
            recalculateCumulativeData(dataHolder.getInstrument().getUnderlyingConid());
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

        underlyingDataHolder.getIvHistoryDependentFields()
                .forEach(field -> messageSender.sendWsMessage(underlyingDataHolder.getType(), underlyingDataHolder.createMessage(field)));
    }

    public void optionPositionChanged(Contract contract, int positionSize) {
        positionLock.lock();
        try {
            int conid = contract.conid();
            PositionDataHolder positionDataHolder = positionMap.get(contract.conid());

            if (positionDataHolder == null) {
                if (positionSize != 0) {
                    Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
                    String underlyingSymbol = contract.symbol();
                    String symbol = contract.localSymbol();
                    Currency currency = Currency.valueOf(contract.currency());
                    Instrument instrument = new Instrument(conid, secType, underlyingSymbol, symbol, currency, null, null);

                    Types.Right right = contract.right();
                    double strike = contract.strike();
                    LocalDate expirationDate = LocalDate.parse(contract.lastTradeDateOrContractMonth(), CoreSettings.IB_DATE_FORMATTER);

                    positionDataHolder = new PositionDataHolder(instrument, ibMktDataRequestIdGen.incrementAndGet(), right, strike, expirationDate, positionSize);
                    positionMap.put(conid, positionDataHolder);

                    ibController.requestContractDetails(ibContractDetailsRequestIdGen.incrementAndGet(), contract);
                }
            } else if (positionSize != 0) {
                if (positionSize != positionDataHolder.getPositionSize()) {
                    positionDataHolder.updatePositionSize(positionSize);

                    messageSender.sendWsMessage(positionDataHolder.getType(), positionDataHolder.createMessage(PositionDataField.POSITION_SIZE));
                    recalculateCumulativeData(positionDataHolder.getInstrument().getUnderlyingConid());
                }
            } else {
                cancelMktData(positionDataHolder);
                positionMap.remove(contract.conid());
                sendReloadRequestMessage(DataHolderType.POSITION);
            }
        } finally {
            positionLock.unlock();
        }
    }

    public void optionPositionContractDetailsReceived(ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();

        int conid = contract.conid();
        PositionDataHolder positionDataHolder = positionMap.get(conid);
        Instrument instrument = positionDataHolder.getInstrument();

        Exchange exchange = Exchange.valueOf(contract.exchange());
        Exchange primaryExchange = contract.primaryExch() != null ? Exchange.valueOf(contract.primaryExch()) : null;

        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        instrument.setExchange(exchange);
        instrument.setPrimaryExchange(primaryExchange);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        sendReloadRequestMessage(DataHolderType.POSITION);
        requestMktData(positionDataHolder);
    }

    public List<UnderlyingDataHolder> getUnderlyingDataHolders() {
        return underlyingDataHolders;
    }

    public List<PositionDataHolder> getSortedPositionDataHolders() {
        List<PositionDataHolder> positionDataHolders = new ArrayList<>(positionMap.values());

        positionDataHolders.sort(Comparator
                .comparing(PositionDataHolder::getDaysToExpiration)
                .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                .thenComparing(PositionDataHolder::getRight)
                .thenComparingDouble(PositionDataHolder::getStrike));

        return positionDataHolders;
    }

    public List<ChainDataHolder> getChainDataHolders() {
        return new ArrayList<>();
    }
}
