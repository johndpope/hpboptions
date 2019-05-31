package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.database.LinearInstrument;
import com.highpowerbear.hpboptions.dataholder.LinearDataHolder;
import com.highpowerbear.hpboptions.field.LinearDataField;
import com.highpowerbear.hpboptions.model.Instrument;
import com.ib.client.Contract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by robertk on 5/28/2019.
 */
@Service
public class LinearService extends AbstractMarketDataService {

    private final HopDao hopDao;

    private final Map<Integer, LinearDataHolder> linearMap = new HashMap<>(); // conid -> linearDataHolder
    private final Map<Integer, LinearDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> linearDataHolder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.LINEAR_IB_REQUEST_ID_INITIAL);

    @Value("${ib.account}")
    private String ibAccount;

    public LinearService(IbController ibController, MessageService messageService, HopDao hopDao) {
        super(ibController, messageService);
        this.hopDao = hopDao;

        ibController.addConnectionListener(this);
        init();
    }

    private void init() {
        for (LinearInstrument li : hopDao.getActiveLinearInstruments()) {
            int conid = li.getConid();

            Instrument instrument = new Instrument(conid, li.getSecType(), li.getSymbolRoot(), li.getSymbol(), li.getCurrency(), li.getExpirationDate());
            instrument.setExchange(li.getExchange());
            instrument.setPrimaryExchange(li.getPrimaryExchange());
            instrument.setMultiplier(li.getMultiplier());
            instrument.setMinTick(li.getMinTick());

            LinearDataHolder ldh = new LinearDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
            ldh.setDisplayRank(li.getDisplayRank());

            linearMap.put(conid, ldh);
        }
    }

    @Override
    public void postConnect() {
        cancelAllMktData();

        linearMap.values().forEach(this::requestMktData);

        linearMap.values().forEach(ldh -> {
            pnlRequestMap.put(ldh.getIbPnlRequestId(), ldh);
            ibController.requestPnlSingle(ldh.getIbPnlRequestId(), ibAccount, ldh.getInstrument().getConid());
        });
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();

        pnlRequestMap.keySet().forEach(ibController::cancelPnlSingle);
        pnlRequestMap.clear();
    }

    public void positionReceived(Contract contract, int positionSize) {
        int conid = contract.conid();
        LinearDataHolder ldh = linearMap.get(conid);

        if (ldh != null) {
            ldh.updatePositionSize(positionSize);
            messageService.sendWsMessage(ldh, LinearDataField.POSITION_SIZE);

            if (positionSize == 0) {
                ldh.resetFields();
                LinearDataField.fields().forEach(field -> messageService.sendWsMessage(ldh, field));
            }
        }
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        LinearDataHolder ldh = pnlRequestMap.get(requestId);

        if (ldh != null) {
            if (ldh.getPositionSize() != 0) {
                ldh.updateUnrealizedPnl(unrealizedPnL);
                messageService.sendWsMessage(ldh, LinearDataField.UNREALIZED_PNL);
            }
        }
    }

    public LinearDataHolder getLinearDataHolder(int conid) {
        return linearMap.get(conid);
    }

    public List<LinearDataHolder> getLinearDataHolders() {
        return linearMap.values().stream().sorted(Comparator
                .comparing(LinearDataHolder::getDisplayRank)).collect(Collectors.toList());
    }
}
