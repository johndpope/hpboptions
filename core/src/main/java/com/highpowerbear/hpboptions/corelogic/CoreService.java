package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.ChainItem;
import com.highpowerbear.hpboptions.corelogic.model.DataHolder;
import com.highpowerbear.hpboptions.corelogic.model.Position;
import com.highpowerbear.hpboptions.corelogic.model.Underlying;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.FieldType;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.ib.client.TickType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

    private final List<Underlying> underlyings = new ArrayList<>();
    private final List<Position> positions = new ArrayList<>();
    private final List<ChainItem> chainItems = new ArrayList<>();

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
            Underlying underlying = new Underlying(or.getUnderlyingInstrument(), ibRequestIdGenerator.incrementAndGet());
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

    public List<Underlying> getUnderlyings() {
        return underlyings;
    }

    public void updateValue(int requestId, int tickTypeIndex, Number value) {
        DataHolder dataHolder = dataMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        if (fieldType == null) {
            return;
        }
        dataHolder.updateValue(fieldType, value);
        messageSender.sendWsMessage(getWsTopic(dataHolder), dataHolder.createMessage(fieldType));

        if (fieldType == FieldType.LAST && CoreUtil.isValidPct(dataHolder.getChangePct())) {
            messageSender.sendWsMessage(getWsTopic(dataHolder), dataHolder.createMessage(FieldType.CHANGE_PCT));
        }
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
        if (dataHolder instanceof Underlying) {
            return CoreSettings.WS_TOPIC_UNDERLYING;
        } else if (dataHolder instanceof Position) {
            return CoreSettings.WS_TOPIC_POSITION;
        } else if (dataHolder instanceof ChainItem) {
            return CoreSettings.WS_TOPIC_CHAIN;
        } else {
            throw new IllegalStateException("unsupported dataHolder type " + dataHolder.getClass().getSimpleName());
        }
    }
}
