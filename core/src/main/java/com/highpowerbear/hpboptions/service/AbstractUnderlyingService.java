package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.dataholder.MarketDataHolder;
import com.highpowerbear.hpboptions.dataholder.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.enums.*;
import com.ib.client.Bar;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by robertk on 5/28/2019.
 */
public abstract class AbstractUnderlyingService extends AbstractMarketDataService implements UnderlyingService {

    private final Map<Integer, MarketDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> dataHolder

    public AbstractUnderlyingService(IbController ibController, MessageService messageService) {
        super(ibController, messageService);
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
