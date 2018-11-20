package com.highpowerbear.hpboptions.process;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.enums.RealtimeFieldType;
import com.highpowerbear.hpboptions.ibclient.IbController;
import com.highpowerbear.hpboptions.process.model.RealtimeData;
import com.ib.client.TickType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.highpowerbear.hpboptions.common.CoreSettings.WS_TOPIC_MKTDATA;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class RealtimeDataController {
    private static final Logger log = LoggerFactory.getLogger(RealtimeDataController.class);

    private final IbController ibController;
    private final MessageSender messageSender;
    private Map<Integer, RealtimeData> realtimeDataMap = new HashMap<>(); // ib request id --> realtimeData

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
        RealtimeFieldType fieldType = RealtimeFieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.createUpdateMessage(fieldType, price);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);

        if (fieldType == RealtimeFieldType.LAST) {
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
        RealtimeFieldType fieldType = RealtimeFieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.createUpdateMessage(fieldType, size);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void tickGenericReceived(int tickerId, int tickTypeIndex, double value) {
        RealtimeData realtimeData = realtimeDataMap.get(tickerId);
        if (realtimeData == null) {
            return;
        }
        RealtimeFieldType fieldType = RealtimeFieldType.getFromTickType(TickType.get(tickTypeIndex));
        String updateMessage = realtimeData.createUpdateMessage(fieldType, value);
        messageSender.sendWsMessage(WS_TOPIC_MKTDATA, updateMessage);
    }

    public void toggleRealtimeData(String contractInfo) {
        RealtimeData realtimeData = realtimeDataMap.values().stream().filter(r -> r.getContractInfo().equals(contractInfo)).findAny().orElse(null);
        int ibRequestId = 0; // TODO

        if (realtimeData == null) {
            realtimeData = new RealtimeData(contractInfo, ibRequestId);

            log.info("requesting realtime data for " + contractInfo);
            realtimeDataMap.put(ibRequestId, realtimeData);
            boolean requested = ibController.requestRealtimeData(ibRequestId, CoreUtil.createContract(contractInfo));

            if (!requested) {
                realtimeDataMap.remove(ibRequestId);
            }
        } else {
            log.info("canceling realtime data for " + contractInfo);

            if (ibController.cancelRealtimeData(ibRequestId)) {
                realtimeDataMap.remove(ibRequestId);
            }
        }
    }

    public void cancelAllMktData() {
        for (Integer requestId : new ArrayList<>(realtimeDataMap.keySet())) {
            RealtimeData realtimeData = realtimeDataMap.get(requestId);
            log.info("canceling realtime data for " + realtimeData.getContractInfo());

            if (ibController.cancelRealtimeData(realtimeData.getIbRequestId())) {
                realtimeDataMap.remove(realtimeData.getIbRequestId());
            }
        }
    }
}
