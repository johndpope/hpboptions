package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.DataHolder;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.FieldType;
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

    private final List<DataHolder> underlyings = new ArrayList<>();
    private final List<DataHolder> positions = new ArrayList<>();
    private final List<DataHolder> chainItems = new ArrayList<>();

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
            DataHolder underlying = new DataHolder(DataHolderType.UNDERLYING, or.getUnderlyingInstrument(), ibRequestIdGenerator.incrementAndGet());
            underlyings.add(underlying);
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
            underlyings.forEach(this::requestData);
        }
    }

    public void disconnect() {
        underlyings.forEach(this::cancelData);
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

    public List<DataHolder> getUnderlyings() {
        return underlyings;
    }

    public void updateValue(int requestId, int tickTypeIndex, Number value) {
        DataHolder dataHolder = dataMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        Set<FieldType> fieldTypes = FieldType.getFieldTypes(TickType.get(tickTypeIndex));
        if (fieldTypes == null) {
            return;
        }
        fieldTypes.stream()
                .filter(FieldType::isBasic)
                .forEach(fieldType -> dataHolder.updateField(fieldType, value));

        fieldTypes.stream()
                .filter(FieldType::isDerived)
                .forEach(dataHolder::calculateField);

        fieldTypes.stream()
                .filter(FieldType::isCreateMessage)
                .forEach(fieldType -> messageSender.sendWsMessage(getWsTopic(dataHolder), dataHolder.createMessage(fieldType)));
    }

    public void requestData(DataHolder dataHolder) {
        dataMap.putIfAbsent(dataHolder.getIbRequestId(), dataHolder);
        ibController.requestMarketData(dataHolder.getIbRequestId(), dataHolder.getInstrument().toIbContract());
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
