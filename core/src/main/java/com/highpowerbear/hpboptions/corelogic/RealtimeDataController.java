package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.corelogic.model.Instrument;
import com.highpowerbear.hpboptions.corelogic.model.RealtimeData;
import com.highpowerbear.hpboptions.enums.FieldType;
import com.highpowerbear.hpboptions.enums.InstrumentGroup;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.ib.client.TickType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_MKTDATA;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class RealtimeDataController {
    private static final Logger log = LoggerFactory.getLogger(RealtimeDataController.class);

    private final InstrumentRepo instrumentRepo;
    private final IbController ibController;
    private final MessageSender messageSender;

    private final Map<Integer, RealtimeData> dataMap = new LinkedHashMap<>(); // ib request id --> realtimeData
    private final AtomicInteger ibRequestId = new AtomicInteger(0);

    @Autowired
    public RealtimeDataController(InstrumentRepo instrumentRepo, IbController ibController, MessageSender messageSender) {
        this.instrumentRepo = instrumentRepo;
        this.ibController = ibController;
        this.messageSender = messageSender;
    }

    public List<RealtimeData> getUndlData() {
        return dataMap.values().stream()
                .filter(rtd -> rtd.getInstrument().getGroup() == InstrumentGroup.UNDERLYING)
                .collect(Collectors.toList());
    }

    public List<RealtimeData> getPositionsData() {
        return dataMap.values().stream()
                .filter(rtd -> rtd.getInstrument().getGroup() == InstrumentGroup.POSITION)
                .collect(Collectors.toList());
    }

    public List<RealtimeData> getChainsData() {
        return dataMap.values().stream()
                .filter(rtd -> rtd.getInstrument().getGroup() == InstrumentGroup.CHAINS)
                .collect(Collectors.toList());
    }

    public void tickPriceReceived(int requestId, int tickTypeIndex, double price) {
        RealtimeData realtimeData = dataMap.get(requestId);
        if (realtimeData == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.updateValue(fieldType, price);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);

        if (fieldType == FieldType.LAST) {
            String updateMessageChangePct = realtimeData.createUpdateMsgChangePct();
            if (updateMessageChangePct != null) {
                messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessageChangePct);
            }
        }
    }

    public void tickSizeReceived(int requestId, int tickTypeIndex, int size) {
        RealtimeData realtimeData = dataMap.get(requestId);
        if (realtimeData == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.updateValue(fieldType, size);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void tickGenericReceived(int requestId, int tickTypeIndex, double value) {
        RealtimeData realtimeData = dataMap.get(requestId);
        if (realtimeData == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.updateValue(fieldType, value);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void requestRealtimeData(Instrument instrument) {
        log.info("requesting realtime data for " + instrument);
        Optional<RealtimeData> realtimeDataOptional = dataMap.values().stream()
                .filter(rtd -> rtd.getInstrument().equals(instrument))
                .findAny();

        if (realtimeDataOptional.isPresent()) {
            RealtimeData realtimeData = realtimeDataOptional.get();
            ibController.requestRealtimeData(realtimeData.getIbRequestId(), instrument.toIbContract());

        } else {
            if (ibController.requestRealtimeData(ibRequestId.incrementAndGet(), instrument.toIbContract())) {
                dataMap.put(ibRequestId.get(), new RealtimeData(instrument, ibRequestId.get()));
            }
        }
    }

    public void cancelRealtimeData(Instrument instrument) {
        Optional<RealtimeData> realtimeDataOptional = dataMap.values().stream()
                .filter(rtd -> rtd.getInstrument().equals(instrument))
                .findAny();

        if (realtimeDataOptional.isPresent()) {
            log.info("canceling realtime data for " + instrument);
            RealtimeData realtimeData = realtimeDataOptional.get();

            if (ibController.cancelRealtimeData(realtimeData.getIbRequestId())) {
                dataMap.remove(realtimeData.getIbRequestId());
            }
        }
    }

    public void requestAllUndlRealtimeData() {
        instrumentRepo.getUndlInstruments().forEach(this::requestRealtimeData);
    }

    public void cancelAllUndlRealtimeData() {
        instrumentRepo.getUndlInstruments().forEach(this::cancelRealtimeData);
    }
}
