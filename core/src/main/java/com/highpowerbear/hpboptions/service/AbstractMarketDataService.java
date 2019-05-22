package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.dataholder.MarketDataHolder;
import com.highpowerbear.hpboptions.dataholder.OptionDataHolder;
import com.highpowerbear.hpboptions.dataholder.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.field.BasicMktDataField;
import com.highpowerbear.hpboptions.field.DerivedMktDataField;
import com.highpowerbear.hpboptions.field.OptionDataField;
import com.ib.client.Bar;
import com.ib.client.ContractDetails;
import com.ib.client.TickType;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robertk on 12/28/2018.
 */
public abstract class AbstractMarketDataService implements MarketDataService {

    protected final IbController ibController;
    protected final MessageService messageService;

    protected final Map<Integer, MarketDataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> dataHolder
    protected final Map<Integer, MarketDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> dataHolder

    public AbstractMarketDataService(IbController ibController, MessageService messageService) {
        this.ibController = ibController;
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

    protected void requestImpliedVolatilityHistory(UnderlyingDataHolder udh) {
        histDataRequestMap.putIfAbsent(udh.getIbHistDataRequestId(), udh);

        ibController.requestHistData(
                udh.getIbHistDataRequestId(),
                udh.getInstrument().createIbContract(),
                LocalDate.now().atStartOfDay().format(HopSettings.IB_DATETIME_FORMATTER),
                IbDurationUnit.YEAR_1.getValue(),
                IbBarSize.DAY_1.getValue(),
                IbHistDataType.OPTION_IMPLIED_VOLATILITY.name(),
                IbTradingHours.REGULAR.getValue());
    }

    @Override
    public void historicalDataReceived(int requestId, Bar bar) {
        MarketDataHolder mdh = histDataRequestMap.get(requestId);
        if (mdh == null) {
            return;
        }
        LocalDate date = LocalDate.parse(bar.time(), HopSettings.IB_DATE_FORMATTER);
        double value = bar.close();

        if (mdh.getType() == DataHolderType.UNDERLYING || mdh.getType() == DataHolderType.SCANNER) {
            UnderlyingDataHolder udh = (UnderlyingDataHolder) mdh;
            udh.addImpliedVolatility(date, value);
        }
    }

    @Override
    public void historicalDataEndReceived(int requestId) {
        MarketDataHolder mdh = histDataRequestMap.get(requestId);
        if (mdh == null) {
            return;
        }
        if (mdh.getType() == DataHolderType.UNDERLYING || mdh.getType() == DataHolderType.SCANNER) {
            UnderlyingDataHolder udh = (UnderlyingDataHolder) mdh;
            udh.impliedVolatilityHistoryCompleted();
            udh.getIvHistoryDependentFields().forEach(field -> messageService.sendWsMessage(udh, field));
        }
    }
}
