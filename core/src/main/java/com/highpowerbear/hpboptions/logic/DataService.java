package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.Bar;
import com.ib.client.TickType;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class DataService {

    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency

    private final List<UnderlyingDataHolder> underlyingDataHolders = new ArrayList<>();
    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder

    private final Map<Integer, DataHolder> mktDataRequestMap = new HashMap<>(); // ib request id -> dataHolder
    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder

    private final AtomicInteger ibMktDataRequestIdGen = new AtomicInteger(0);
    private final AtomicInteger ibHistDataRequestIdGen = new AtomicInteger(1000000);

    private final Comparator<PositionDataHolder> positionDataHolderComparator;

    @Autowired
    public DataService(CoreDao coreDao, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.messageSender = messageSender;

        positionDataHolderComparator = Comparator
                .comparing(PositionDataHolder::getDaysToExpiration)
                .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                .thenComparing(Comparator.comparing(PositionDataHolder::getRight).reversed()) // Put, Call
                .thenComparingDouble(PositionDataHolder::getStrike);
    }

    @PostConstruct
    public void init() {
        List<Underlying> underlyings = coreDao.getActiveUnderlyings();

        for (Underlying underlying : underlyings) {
            UnderlyingDataHolder underlyingDataHolder = new UnderlyingDataHolder(
                    underlying.createInstrument(),
                    ibMktDataRequestIdGen.incrementAndGet(),
                    ibHistDataRequestIdGen.incrementAndGet());

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
        histDataRequestMap.put(underlyingDataHolder.getIbHistDataRequestId(), underlyingDataHolder);

        ibController.requestHistData(
                underlyingDataHolder.getIbHistDataRequestId(),
                underlyingDataHolder.getInstrument().toIbContract(),
                LocalDate.now().atStartOfDay().format(CoreSettings.IB_HIST_DATA_DATETIME_FORMATTER),
                IbDurationUnit.YEAR_1.getValue(),
                IbBarSize.DAY_1.getValue(),
                IbHistDataType.OPTION_IMPLIED_VOLATILITY.name(),
                IbTradingHours.REGULAR.getValue());
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
        if (tickType != TickType.LAST_OPTION.index()) {
            return;
        }
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        OptionDataHolder optionDataHolder = (OptionDataHolder) dataHolder;
        optionDataHolder.updateOptionData(delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);

        OptionDataField.getValues().stream()
                .filter(dataHolder::isSendMessage)
                .forEach(field -> messageSender.sendWsMessage(dataHolder.getType(), dataHolder.createMessage(field)));
    }

    public void historicalDataReceived(int requestId, Bar bar) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);

        LocalDate date = LocalDate.parse(bar.time(), CoreSettings.IB_HIST_DATA_DATE_FORMATTER);
        double value = bar.close();
        underlyingDataHolder.addImpliedVolatility(date, value);
    }

    public void historicalDataEnd(int requestId) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);
        underlyingDataHolder.impliedVolatilityHistoryCompleted();

        underlyingDataHolder.getIvHistoryDependentFields()
                .forEach(field -> messageSender.sendWsMessage(underlyingDataHolder.getType(), underlyingDataHolder.createMessage(field)));
    }

    public void optionPositionChanged(Instrument instrument, Types.Right right, double strike, int positionSize) {
        int conid = instrument.getConid();
        PositionDataHolder positionDataHolder = positionMap.get(conid);

        if (positionDataHolder == null) {
            if (positionSize != 0) {
                positionDataHolder = new PositionDataHolder(instrument, ibMktDataRequestIdGen.incrementAndGet(), right, strike, positionSize);
                positionMap.put(conid, positionDataHolder);

                requestMktData(positionDataHolder);
            } else {
                return;
            }
        } else if (positionSize != 0) {
            positionDataHolder.updatePositionSize(positionSize);
        } else {
            cancelMktData(positionDataHolder);
            positionMap.remove(conid);
        }

        messageSender.sendWsMessage(positionDataHolder.getType(), "position changed " + instrument.getSymbol());
    }

    public List<UnderlyingDataHolder> getUnderlyingDataHolders() {
        return underlyingDataHolders;
    }

    public List<PositionDataHolder> getPositionDataHolders() {
        List<PositionDataHolder> positionDataHolders = new ArrayList<>(positionMap.values());
        positionDataHolders.sort(positionDataHolderComparator);

        return positionDataHolders;
    }

    public List<ChainDataHolder> getChainDataHolders() {
        return new ArrayList<>();
    }
}
