package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.DataHolder;
import com.highpowerbear.hpboptions.corelogic.model.OptionDataHolder;
import com.highpowerbear.hpboptions.corelogic.model.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.ib.client.TickType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class CoreService {

    private final CoreDao coreDao;
    private final MessageSender messageSender;

    private IbController ibController; // prevent circular dependency

    private final List<DataHolder> underlyingDataHolders = new ArrayList<>();
    private final List<DataHolder> positionDataHolders = new ArrayList<>();
    private final List<DataHolder> chainDataHolders = new ArrayList<>();

    private final Map<Integer, DataHolder> dataMap = new LinkedHashMap<>(); // ib request id --> dataHolder
    private final AtomicInteger ibRequestIdGenerator = new AtomicInteger(0);

    @Autowired
    public CoreService(CoreDao coreDao, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        List<Underlying> underlyings = coreDao.getActiveUnderlyings();

        underlyings.forEach(underlying -> {
            UnderlyingDataHolder underlyingDataHolder = new UnderlyingDataHolder(underlying.createInstrument(), ibRequestIdGenerator.incrementAndGet());
            underlyingDataHolders.add(underlyingDataHolder);
        });
    }

    public void setIbController(IbController ibController) {
        this.ibController = ibController;
    }

    public void connect() {
        ibController.connect();

        if (isConnected()) {
            dataMap.keySet().forEach(ibController::cancelMarketData);
            dataMap.clear();
            underlyingDataHolders.forEach(this::requestData);
        }
    }

    public void disconnect() {
        underlyingDataHolders.forEach(this::cancelData);
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

    @Scheduled(cron="0 0 * * * MON-FRI")
    private void saveImpliedVolatility() {
        // TODO take into account underlying market open
    }

    public List<DataHolder> getUnderlyingDataHolders() {
        return underlyingDataHolders;
    }

    public List<DataHolder> getPositionDataHolders() {
        return positionDataHolders;
    }

    public List<DataHolder> getChainDataHolders() {
        return chainDataHolders;
    }

    public void updateValue(int requestId, int tickType, Number value) {
        DataHolder dataHolder = dataMap.get(requestId);

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
        DataHolder dataHolder = dataMap.get(requestId);
        OptionDataHolder optionDataHolder = (OptionDataHolder) dataHolder;

        optionDataHolder.updateOptionData(delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);
        messageSender.sendWsMessage(getWsTopic(dataHolder), optionDataHolder.createOptionDataMessage());
    }

    public void requestData(DataHolder dataHolder) {
        dataMap.putIfAbsent(dataHolder.getIbRequestId(), dataHolder);
        ibController.requestMarketData(dataHolder.getIbRequestId(), dataHolder.getInstrument().toIbContract(), dataHolder.getGenericTicks());
    }

    public void cancelData(DataHolder dataHolder) {
        ibController.cancelMarketData(dataHolder.getIbRequestId());
        dataMap.remove(dataHolder.getIbRequestId());
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
