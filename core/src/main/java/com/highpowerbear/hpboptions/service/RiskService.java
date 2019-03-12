package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.database.RiskEvent;
import com.highpowerbear.hpboptions.dataholder.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.enums.OrderSource;
import com.highpowerbear.hpboptions.field.DataField;
import com.highpowerbear.hpboptions.field.UnderlyingDataField;
import com.ib.client.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final OrderService orderService;
    private final MessageService messageService;

    private final Map<Integer, Set<DataField>> riskEventMap = new HashMap<>(); // underlying conid -> fields for which risk events have already triggered in the last 24h

    @Value("${ib.account}")
    private String ibAccount;

    @Autowired
    public RiskService(HopDao hopDao, UnderlyingService underlyingService, OrderService orderService, MessageService messageService) {
        this.hopDao = hopDao;
        this.underlyingService = underlyingService;
        this.orderService = orderService;
        this.messageService = messageService;

        hopDao.getActiveUnderlyings().forEach(u -> riskEventMap.put(u.getConid(), new HashSet<>()));
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        riskEventMap.values().forEach(Set::clear);
    }

    @JmsListener(destination = HopSettings.JMS_DEST_UNDERLYING_RISK_DATA_CALCULATED)
    public void underlyingRiskDataCalculated(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingService.getUnderlyingDataHolder(underlyingConid);

        udh.getRiskCalculationLock().lock();
        try {
            for (UnderlyingDataField field : UnderlyingDataField.riskDataFields()) {
                Number fieldValue = udh.getCurrent(field);

                if (field.thresholdBreached(fieldValue)) {
                    if (field == UnderlyingDataField.PORTFOLIO_DELTA_ONE_PCT &&
                            udh.getCfdInstrument() != null &&
                            udh.isDeltaHedge() &&
                            udh.isDeltaHedgeDue() &&
                            udh.isMarketOpen() &&
                            !orderService.hasWorkingOrder(udh.getCfdInstrument().getConid())) {

                        udh.updateLastDeltaHedgeTime();
                        int quantity = HopSettings.DELTA_HEDGE_QUANTITY_STEP;
                        Types.Action action = udh.getPortfolioDeltaOnePct() > 0 ? Types.Action.SELL : Types.Action.BUY;

                        String resolution = "delta hedge " + action.name() + " " + quantity;
                        log.info(resolution);

                        orderService.createAndSendAdaptiveCfdOrder(underlyingConid, action, quantity, OrderSource.RDH);
                        createRiskEvent(udh, field, resolution);

                    } else if (!riskEventMap.get(underlyingConid).contains(field)) {
                        createRiskEvent(udh, field, null);
                    }
                    riskEventMap.get(underlyingConid).add(field);
                }
            }
        } finally {
            udh.getRiskCalculationLock().unlock();
        }
    }

    private void createRiskEvent(UnderlyingDataHolder udh, DataField dataField, String resolution) {
        RiskEvent riskEvent = new RiskEvent();

        riskEvent.setDate(LocalDateTime.now());
        riskEvent.setUnderlyingConid(udh.getInstrument().getConid());
        riskEvent.setUnderlyingSymbol(udh.getInstrument().getSymbol());
        riskEvent.setDataField(HopUtil.toCamelCase(dataField.name()));
        riskEvent.setFieldValue(String.valueOf(udh.getCurrent(dataField)));
        riskEvent.setFieldThreshold(dataField.getRiskThreshold().toString());
        riskEvent.setResolution(resolution);
        riskEvent.setIbAccount(ibAccount);

        hopDao.createRiskEvent(riskEvent);

        String subject = "Risk event " + riskEvent.getUnderlyingSymbol();
        String text = "Account " + ibAccount +
                ", " + riskEvent.getDataField() + "=" + riskEvent.getFieldValue() +
                ", threshold=" + riskEvent.getFieldThreshold() +
                ", resolution=" + riskEvent.getResolution();

        messageService.sendEmailMessage(subject, text);
    }
}
