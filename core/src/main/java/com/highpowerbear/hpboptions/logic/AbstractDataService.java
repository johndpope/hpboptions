package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.OptionDataField;
import com.highpowerbear.hpboptions.model.DataHolder;
import com.highpowerbear.hpboptions.model.OptionDataHolder;
import com.ib.client.ContractDetails;
import com.ib.client.TickType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robertk on 12/28/2018.
 */
public abstract class AbstractDataService implements DataService {

    protected final IbController ibController;
    protected final CoreDao coreDao;
    protected final MessageService messageService;

    private final Map<Integer, DataHolder> mktDataRequestMap = new ConcurrentHashMap<>(); // ib request id -> dataHolder

    public AbstractDataService(IbController ibController, CoreDao coreDao, MessageService messageService) {
        this.ibController = ibController;
        this.coreDao = coreDao;
        this.messageService = messageService;
    }

    protected void requestMktData(DataHolder dataHolder) {
        int requestId = dataHolder.getIbMktDataRequestId();

        mktDataRequestMap.put(requestId, dataHolder);
        ibController.requestMktData(requestId, dataHolder.createIbContract(), dataHolder.getGenericTicks());
    }

    protected void cancelMktData(DataHolder dataHolder) {
        int requestId = dataHolder.getIbMktDataRequestId();

        ibController.cancelMktData(requestId);
        mktDataRequestMap.remove(requestId);
    }

    protected void cancelAllMktData() {
        mktDataRequestMap.keySet().forEach(ibController::cancelMktData);
        mktDataRequestMap.clear();
    }

    @Override
    public void mktDataReceived(int requestId, int tickType, Number value) {
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        BasicMktDataField basicField = BasicMktDataField.basicField(TickType.get(tickType));
        if (basicField == null) {
            return;
        }
        dataHolder.updateField(basicField, value);
        DerivedMktDataField.derivedFields(basicField).forEach(dataHolder::calculateField);

        messageService.sendWsMessage(dataHolder, basicField);
        DerivedMktDataField.derivedFields(basicField).forEach(derivedField -> messageService.sendWsMessage(dataHolder, derivedField));
    }

    @Override
    public void optionDataReceived(int requestId, TickType tickType, double delta, double gamma, double vega, double theta, double impliedVolatility, double optionPrice, double underlyingPrice) {
        if (tickType == TickType.LAST_OPTION) {
            return;
        }
        DataHolder dataHolder = mktDataRequestMap.get(requestId);
        if (dataHolder == null) {
            return;
        }
        OptionDataHolder optionDataHolder = (OptionDataHolder) dataHolder;
        optionDataHolder.updateOptionData(tickType, delta, gamma, vega, theta, impliedVolatility, optionPrice, underlyingPrice);

        if (tickType == TickType.MODEL_OPTION) { // update on bid, ask or model, but recalculate and send message only on model
            optionDataHolder.recalculateOptionData();
            OptionDataField.fields().forEach(field -> messageService.sendWsMessage(dataHolder, field));

            modelOptionDataReceived(optionDataHolder);
        }
    }

    @Override
    public abstract void contractDetailsReceived(ContractDetails contractDetails);

    @Override
    public void contractDetailsEndReceived(int requestId) {
    }

    protected void modelOptionDataReceived(OptionDataHolder optionDataHolder) {
    }
}
