package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.DataHolder;
import com.highpowerbear.hpboptions.corelogic.model.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.BasicField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedField;
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
        List<OptionRoot> optionRoots = coreDao.getActiveOptionRoots();

        optionRoots.forEach(or -> {
            UnderlyingDataHolder underlyingDataHolder = new UnderlyingDataHolder(or.getUnderlyingInstrument(), ibRequestIdGenerator.incrementAndGet());
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

    public List<DataHolder> getUnderlyingDataHolders() {
        return underlyingDataHolders;
    }

    public List<DataHolder> getPositionDataHolders() {
        return positionDataHolders;
    }

    public List<DataHolder> getChainDataHolders() {
        return chainDataHolders;
    }

    public void updateValue(int requestId, int tickTypeIndex, Number value) {
        DataHolder dataHolder = dataMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        BasicField basicField = BasicField.getBasicField(TickType.get(tickTypeIndex));
        if (basicField == null) {
            return;
        }
        dataHolder.updateField(basicField, value);
        DerivedField.getDerivedFields(basicField).forEach(dataHolder::calculateField);

        String wsTopic = getWsTopic(dataHolder);
        if (dataHolder.isDisplayed(basicField)) {
            messageSender.sendWsMessage(wsTopic, dataHolder.createMessage(basicField));
        }

        DerivedField.getDerivedFields(basicField).stream()
                .filter(dataHolder::isDisplayed)
                .forEach(derivedField -> messageSender.sendWsMessage(wsTopic, dataHolder.createMessage(derivedField)));
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
