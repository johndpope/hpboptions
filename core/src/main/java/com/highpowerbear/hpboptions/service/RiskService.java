package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.DataField;
import com.highpowerbear.hpboptions.model.UnderlyingDataHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by robertk on 1/28/2019.
 */
@Service
public class RiskService {
    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final HopDao hopDao;
    private final UnderlyingService underlyingService;

    private final Map<Integer, Set<DataField>> riskEventMap = new HashMap<>(); // underlying conid -> fields for which risk events have already triggered in the last 24h

    @Autowired
    public RiskService(HopDao hopDao, UnderlyingService underlyingService) {
        this.hopDao = hopDao;
        this.underlyingService = underlyingService;

        hopDao.getActiveUnderlyings().forEach(u -> riskEventMap.put(u.getConid(), new HashSet<>()));
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        riskEventMap.values().forEach(Set::clear);
    }

    @JmsListener(destination = HopSettings.JMS_DEST_RISK_DATA_RECALCULATED)
    public void riskDataRecalculated(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingService.getUnderlyingDataHolder(underlyingConid);

        udh.getThresholdBreachedFields().entrySet().stream()
                .filter(entry -> !riskEventMap.get(underlyingConid).contains(entry.getKey()))
                .forEach(entry -> {
                    riskEventMap.get(underlyingConid).add(entry.getKey());
                    // TODO
                });
    }
}
