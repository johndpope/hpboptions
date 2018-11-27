package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.*;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.FieldType;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.ib.client.TickType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class DataController {
    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    private final CoreDao coreDao;
    private final IbController ibController;
    private final MessageSender messageSender;

    private final List<Underlying> underlyings = new ArrayList<>();
    private final List<Position> positions = new ArrayList<>();
    private final List<ChainItem> chainItems = new ArrayList<>();

    private final Map<Integer, DataHolder> dataMap = new LinkedHashMap<>(); // ib request id --> dataHolder
    private final AtomicInteger ibRequestIdGenerator = new AtomicInteger(0);

    @Autowired
    public DataController(CoreDao coreDao, IbController ibController, MessageSender messageSender) {
        this.coreDao = coreDao;
        this.ibController = ibController;
        this.messageSender = messageSender;
    }

    @PostConstruct
    public void init() {
        initUnderlyings();
    }

    public void initUnderlyings() {
        underlyings.clear();
        List<OptionRoot> optionRoots = coreDao.getActiveOptionRoots();
        optionRoots.forEach(or -> {
            Instrument instrument = new Instrument(or.getUndlSecType(), or.getUndlSymbol(), or.getUndlSymbol(), or.getCurrency(), or.getUndlExchange());
            Underlying underlying = new Underlying(instrument, ibRequestIdGenerator.incrementAndGet());
            underlyings.add(underlying);
        });
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
        dataHolder.updateValue(fieldType, value);
        messageSender.sendWsMessage(getWsTopic(dataHolder), dataHolder.createMessage(fieldType));

        if (fieldType == FieldType.LAST) {
            messageSender.sendWsMessage(getWsTopic(dataHolder), dataHolder.createMessage(FieldType.CHANGE_PCT));
        }
    }

    public void reset() {
        dataMap.keySet().forEach(ibController::cancelRealtimeData);
        dataMap.clear();
    }

    public void requestData(DataHolder dataHolder) {
        log.info("requesting realtime data for " + dataHolder.getInstrument());
        dataMap.putIfAbsent(dataHolder.getIbRequestId(), dataHolder);
        ibController.requestRealtimeData(dataHolder.getIbRequestId(), dataHolder.getInstrument().toIbContract());
    }

    public void cancelData(DataHolder dataHolder) {
        log.info("canceling realtime data for " + dataHolder.getInstrument());
        ibController.cancelRealtimeData(dataHolder.getIbRequestId());
        dataMap.remove(dataHolder.getIbRequestId());
    }

    public String getWsTopic(DataHolder dataHolder) {
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
