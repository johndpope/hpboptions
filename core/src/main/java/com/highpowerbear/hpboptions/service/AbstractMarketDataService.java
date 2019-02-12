package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.highpowerbear.hpboptions.dataholder.MarketDataHolder;
import com.highpowerbear.hpboptions.dataholder.OptionDataHolder;
import com.ib.client.ContractDetails;
import com.ib.client.TickType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robertk on 12/28/2018.
 */
public abstract class AbstractMarketDataService implements MarketDataService {

    protected final IbController ibController;
    protected final HopDao hopDao;
    protected final MessageService messageService;

    protected final Map<Integer, MarketDataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> dataHolder

    public AbstractMarketDataService(IbController ibController, HopDao hopDao, MessageService messageService) {
        this.ibController = ibController;
        this.hopDao = hopDao;
        this.messageService = messageService;
    }

    protected void requestMktData(MarketDataHolder mdh) {
        int requestId = mdh.getIbMktDataRequestId();

        mktDataRequestMap.put(requestId, mdh);
        ibController.requestMktData(requestId, mdh.getInstrument().createIbContract(), mdh.getGenericTicks());
    }

    protected void cancelMktData(MarketDataHolder mdh) {
        int requestId = mdh.getIbMktDataRequestId();

        ibController.cancelMktData(requestId);
        mktDataRequestMap.remove(requestId);
    }

    protected void cancelAllMktData() {
        mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
        mktDataRequestMap.clear();
    }

    @Override
    public void mktDataReceived(int requestId, int tickType, Number value) {
        MarketDataHolder mdh = mktDataRequestMap.get(requestId);
        if (mdh == null) {
            return;
        }
        BasicMktDataField basicField = BasicMktDataField.basicField(TickType.get(tickType));
        if (basicField == null) {
            return;
        }
        mdh.updateField(basicField, value);
        DerivedMktDataField.derivedFields(basicField).forEach(mdh::calculateField);

        messageService.sendWsMessage(mdh, basicField);
        DerivedMktDataField.derivedFields(basicField).forEach(derivedField -> messageService.sendWsMessage(mdh, derivedField));
    }

    @Override
    public void optionDataReceived(int requestId, TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        if (tickType == TickType.LAST_OPTION) {
            return;
        }
        MarketDataHolder mdh = mktDataRequestMap.get(requestId);
        if (mdh == null) {
            return;
        }
        if (mdh.getType() == DataHolderType.POSITION || mdh.getType() == DataHolderType.CHAIN) {
            OptionDataHolder optionDataHolder = (OptionDataHolder) mdh;
            optionDataHolder.optionDataReceived(tickType, delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);

            if (tickType == TickType.MODEL_OPTION) { // update on bid, ask or model, but recalculate and send message only on model
                optionDataHolder.recalculateOptionData();
                OptionDataField.fields().forEach(field -> messageService.sendWsMessage(mdh, field));

                modelOptionDataReceived(optionDataHolder);
            }
        }
    }

    @Override
    public void contractDetailsReceived(int requestId, ContractDetails contractDetails) {
    }

    @Override
    public void contractDetailsEndReceived(int requestId) {
    }

    protected void modelOptionDataReceived(OptionDataHolder optionDataHolder) {
    }
}
