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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_MKTDATA;

/**
 * Created by robertk on 11/5/2018.
 */
@Component
public class DataController {
    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    private final IbController ibController;
    private final MessageSender messageSender;

    private final Map<Integer, RealtimeData> dataMap = new LinkedHashMap<>(); // ib request id --> realtimeData
    private final AtomicInteger ibRequestId = new AtomicInteger(0);

    @Autowired
    public DataController(IbController ibController, MessageSender messageSender) {
        this.ibController = ibController;
        this.messageSender = messageSender;
    }

    public List<RealtimeData> getUndlData() {
        return dataMap.values().stream()
                .filter(data -> data.getInstrument().getGroup() == InstrumentGroup.UNDERLYING)
                .collect(Collectors.toList());
    }

    public List<RealtimeData> getPositionsData() {
        return dataMap.values().stream()
                .filter(data -> data.getInstrument().getGroup() == InstrumentGroup.POSITION)
                .collect(Collectors.toList());
    }

    public List<RealtimeData> getChainsData() {
        return dataMap.values().stream()
                .filter(data -> data.getInstrument().getGroup() == InstrumentGroup.CHAINS)
                .collect(Collectors.toList());
    }

    public void tickPriceReceived(int requestId, int tickTypeIndex, double price) {
        RealtimeData data = dataMap.get(requestId);
        if (data == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = data.updateValue(fieldType, price);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);

        if (fieldType == FieldType.LAST) {
            String updateMessageChangePct = data.createUpdateMsgChangePct();
            if (updateMessageChangePct != null) {
                messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessageChangePct);
            }
        }
    }

    public void tickSizeReceived(int requestId, int tickTypeIndex, int size) {
        RealtimeData data = dataMap.get(requestId);
        if (data == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = data.updateValue(fieldType, size);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void tickGenericReceived(int requestId, int tickTypeIndex, double value) {
        RealtimeData data = dataMap.get(requestId);
        if (data == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = data.updateValue(fieldType, value);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void reset() {
        dataMap.values().forEach(data -> ibController.cancelRealtimeData(data.getIbRequestId()));
        dataMap.clear();
    }

    public void requestData(Instrument instrument) {
        log.info("requesting realtime data for " + instrument);
        Optional<RealtimeData> dataOptional = dataMap.values().stream()
                .filter(data -> data.getInstrument().equals(instrument))
                .findAny();

        if (dataOptional.isPresent()) {
            RealtimeData data = dataOptional.get();
            ibController.requestRealtimeData(data.getIbRequestId(), instrument.toIbContract());

        } else {
            if (ibController.requestRealtimeData(ibRequestId.incrementAndGet(), instrument.toIbContract())) {
                dataMap.put(ibRequestId.get(), new RealtimeData(instrument, ibRequestId.get()));
            }
        }
    }

    public void cancelData(Instrument instrument) {
        Optional<RealtimeData> dataOptional = dataMap.values().stream()
                .filter(data -> data.getInstrument().equals(instrument))
                .findAny();

        if (dataOptional.isPresent()) {
            log.info("canceling realtime data for " + instrument);
            RealtimeData data = dataOptional.get();

            if (ibController.cancelRealtimeData(data.getIbRequestId())) {
                dataMap.remove(data.getIbRequestId());
            }
        }
    }
}
