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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class CoreService {

    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency

    private final List<UnderlyingDataHolder> underlyingDataHolders = new ArrayList<>();
    private final List<PositionDataHolder> positionDataHolders = new ArrayList<>();
    private final List<ChainDataHolder> chainDataHolders = new ArrayList<>();

    private final Map<Integer, DataHolder> mktDataRequestMap = new LinkedHashMap<>(); // ib request id --> dataHolder
    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new LinkedHashMap<>(); // ib request id --> underlyingDataHolder

    private final AtomicInteger ibMktDataRequestIdGen = new AtomicInteger(0);
    private final AtomicInteger ibHistDataRequestIdGen = new AtomicInteger(1000000);

    @Autowired
    public CoreService(CoreDao coreDao, MessageSender messageSender) {
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

            underlyingDataHolders.add(underlyingDataHolder);
        }
    }

    public void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    public void connect() {
        ibController.connect();

        if (isConnected()) {
            mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
            mktDataRequestMap.clear();
            underlyingDataHolders.forEach(this::requestMktData);
            underlyingDataHolders.forEach(this::requestImpliedVolatilityHistory);
        }
    }

    public void disconnect() {
        underlyingDataHolders.forEach(this::cancelMktData);
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
        mktDataRequestMap.putIfAbsent(dataHolder.getIbMktDataRequestId(), dataHolder);
        ibController.requestMktData(dataHolder.getIbMktDataRequestId(), dataHolder.getInstrument().toIbContract(), dataHolder.getGenericTicks());
    }

    private void cancelMktData(DataHolder dataHolder) {
        ibController.cancelMktData(dataHolder.getIbMktDataRequestId());
        mktDataRequestMap.remove(dataHolder.getIbMktDataRequestId());
    }

    private void requestImpliedVolatilityHistory(UnderlyingDataHolder underlyingDataHolder) {
        histDataRequestMap.putIfAbsent(underlyingDataHolder.getIbHistDataRequestId(), underlyingDataHolder);

        ibController.requestHistData(
                underlyingDataHolder.getIbHistDataRequestId(),
                underlyingDataHolder.getInstrument().toIbContract(),
                LocalDate.now().format(CoreSettings.IB_HIST_DATA_DATE_FORMATTER),
                IbDurationUnit.YEAR_1.getValue(),
                IbBarSize.DAY_1.getValue(),
                IbHistDataType.OPTION_IMPLIED_VOLATILITY.name(),
                IbTradingHours.REGULAR.getValue());
    }

    public void updateMktData(int requestId, int tickType, Number value) {
        DataHolder dataHolder = mktDataRequestMap.get(requestId);

        BasicMktDataField basicField = BasicMktDataField.getBasicField(TickType.get(tickType));
        if (basicField == null) {
            return;
        }
        dataHolder.updateField(basicField, value);
        DerivedMktDataField.getDerivedFields(basicField).forEach(dataHolder::calculateField);

        String wsTopic = getWsTopic(dataHolder);
        if (dataHolder.isDisplayed(basicField)) {
            messageSender.sendWsMessage(wsTopic, dataHolder.createMessage(basicField));
        }

        DerivedMktDataField.getDerivedFields(basicField).stream()
                .filter(dataHolder::isDisplayed)
                .forEach(derivedField -> messageSender.sendWsMessage(wsTopic, dataHolder.createMessage(derivedField)));
    }

    public void updateOptionData(int requestId, int tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        if (tickType != TickType.LAST_OPTION.index()) {
            return;
        }
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        OptionDataHolder optionDataHolder = (OptionDataHolder) dataHolder;

        optionDataHolder.updateOptionData(delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);
        messageSender.sendWsMessage(getWsTopic(dataHolder), optionDataHolder.createOptionDataMessage());
    }

    public void historicalDataReceived(int requestId, Bar bar) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);
        underlyingDataHolder.addImpliedVolatilityHistoricValue(bar);
    }

    public void historicalDataEnd(int requestId) {
        UnderlyingDataHolder underlyingDataHolder = histDataRequestMap.get(requestId);
        underlyingDataHolder.calculateImpliedVolatilityRank();
    }

    public List<UnderlyingDataHolder> getUnderlyingDataHolders() {
        return underlyingDataHolders;
    }

    public List<PositionDataHolder> getPositionDataHolders() {
        return positionDataHolders;
    }

    public List<ChainDataHolder> getChainDataHolders() {
        return chainDataHolders;
    }

    private String getWsTopic(DataHolder dataHolder) {
        if (dataHolder.getType() == DataHolderType.UNDERLYING) {
            return CoreSettings.WS_TOPIC_UNDERLYING;

        } else if (dataHolder.getType() == DataHolderType.POSITION) {
            return CoreSettings.WS_TOPIC_POSITION;

        } else if (dataHolder.getType() == DataHolderType.CHAIN) {
            return CoreSettings.WS_TOPIC_CHAIN;

        } else {
            throw new IllegalStateException("unsupported dataHolder type " + dataHolder.getType());
        }
    }
}
