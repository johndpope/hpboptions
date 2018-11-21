package com.highpowerbear.hpboptions.process;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.enums.FieldType;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.highpowerbear.hpboptions.process.model.Instrument;
import com.highpowerbear.hpboptions.process.model.RealtimeData;
import com.ib.client.TickType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
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
public class RealtimeDataController {
    private static final Logger log = LoggerFactory.getLogger(RealtimeDataController.class);

    private final IbController ibController;
    private final MessageSender messageSender;

    private Map<Integer, RealtimeData> realtimeDataMap = new HashMap<>(); // ib request id --> realtimeData
    private AtomicInteger ibRequestId = new AtomicInteger(0);

    @Autowired
    public RealtimeDataController(IbController ibController, MessageSender messageSender) {
        this.ibController = ibController;
        this.messageSender = messageSender;
    }

    public List<RealtimeData> getRealtimeDataList() {
        return new ArrayList<>(realtimeDataMap.keySet()).stream()
                .sorted()
                .map(rid -> realtimeDataMap.get(rid))
                .collect(Collectors.toList());
    }

    public void tickPriceReceived(int tickerId, int tickTypeIndex, double price) {
        RealtimeData realtimeData = realtimeDataMap.get(tickerId);
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

    public void tickSizeReceived(int tickerId, int tickTypeIndex, int size) {
        RealtimeData realtimeData = realtimeDataMap.get(tickerId);
        if (realtimeData == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.updateValue(fieldType, size);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void tickGenericReceived(int tickerId, int tickTypeIndex, double value) {
        RealtimeData realtimeData = realtimeDataMap.get(tickerId);
        if (realtimeData == null) {
            return;
        }
        FieldType fieldType = FieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.updateValue(fieldType, value);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void requestRealtimeData(Instrument instrument) {
        Optional<RealtimeData> realtimeDataOptional = realtimeDataMap.values().stream().filter(rtd -> rtd.getInstrument().equals(instrument)).findAny();

        if (!realtimeDataOptional.isPresent()) {
            log.info("requesting realtime data for " + instrument);

            if (ibController.requestRealtimeData(ibRequestId.incrementAndGet(), CoreUtil.contract(instrument))) {
                realtimeDataMap.put(ibRequestId.get(), new RealtimeData(instrument, ibRequestId.get()));
            }
        }
    }

    public void cancelRealtimeData(Instrument instrument) {
        Optional<RealtimeData> realtimeDataOptional = realtimeDataMap.values().stream().filter(rtd -> rtd.getInstrument().equals(instrument)).findAny();

        if (realtimeDataOptional.isPresent()) {
            log.info("canceling realtime data for " + instrument);
            RealtimeData realtimeData = realtimeDataOptional.get();

            if (ibController.cancelRealtimeData(realtimeData.getIbRequestId())) {
                realtimeDataMap.remove(realtimeData.getIbRequestId());
            }
        }
    }
}
