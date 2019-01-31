package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.common.MessageService;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.database.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.Bar;
import com.ib.client.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class UnderlyingService extends AbstractDataService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(UnderlyingService.class);

    private final AccountSummary accountSummary;
    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new HashMap<>(); // conid -> underlyingDataHolder
    private final Map<Integer, Map<Integer, PositionDataHolder>> underlyingPositionMap = new ConcurrentHashMap<>(); // underlying conid -> (position conid -> positionDataHolder)

    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.UNDERLYING_IB_REQUEST_ID_INITIAL);

    @Value("${fixer.access-key}") private String fixerAccessKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private ExchangeRates exchangeRates;

    @Autowired
    public UnderlyingService(IbController ibController, HopDao hopDao, MessageService messageService) {
        super(ibController, hopDao, messageService);

        ibController.addConnectionListener(this);
        accountSummary = new AccountSummary(ibRequestIdGen.incrementAndGet());
    }

    @PostConstruct
    private void init() {
        for (Underlying underlying : hopDao.getActiveUnderlyings()) {
            int conid = underlying.getConid();

            Instrument instrument = new Instrument(conid, underlying.getSecType(), underlying.getSymbol(), underlying.getCurrency());
            instrument.setExchange(underlying.getExchange());
            instrument.setPrimaryExchange(underlying.getPrimaryExchange());

            UnderlyingDataHolder udh = new UnderlyingDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
            udh.setDisplayRank(underlying.getDisplayRank());

            underlyingMap.put(conid, udh);
            underlyingPositionMap.put(conid, new ConcurrentHashMap<>());
        }

        retrieveExchangeRates();
    }

    @Override
    public void postConnect() {
        cancelAllMktData();

        underlyingMap.values().forEach(this::requestMktData);
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
        ibController.requestAccountSummary(accountSummary.getIbRequestId(), accountSummary.getTags());
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
        ibController.cancelAccountSummary(accountSummary.getIbRequestId());
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        retrieveExchangeRates();
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
        ibController.requestAccountSummary(accountSummary.getIbRequestId(), accountSummary.getTags());
    }

    private void retrieveExchangeRates() {
        String date = HopUtil.formatExchangeRateDate(LocalDate.now());
        String query = HopSettings.EXCHANGE_RATES_URL + "/" + date + "?access_key=" + fixerAccessKey + "&symbols=" + HopSettings.EXCHANGE_RATES_SYMBOLS;

        exchangeRates = restTemplate.getForObject(query, ExchangeRates.class);
        log.info("retrieved exchange rates " + exchangeRates);
    }

    private void requestImpliedVolatilityHistory(UnderlyingDataHolder udh) {
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

    public void addPosition(int underlyingConid, PositionDataHolder pdh) {
        if (underlyingMap.containsKey(underlyingConid)) {
            underlyingPositionMap.get(underlyingConid).put(pdh.getInstrument().getConid(), pdh);
            recalculatePositionsSum(underlyingConid);
        }
    }

    public void removePosition(int underlyingConid, int positionConid) {
        if (underlyingMap.containsKey(underlyingConid)) {
            underlyingPositionMap.get(underlyingConid).remove(positionConid);
            recalculatePositionsSum(underlyingConid);
        }
    }

    public void recalculatePositionsSum(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }

        int putsSum = underlyingPositionMap.get(underlyingConid).values().stream()
                .filter(pdh -> pdh.getInstrument().getRight() == Types.Right.Put)
                .mapToInt(PositionDataHolder::getPositionSize)
                .sum();

        int callsSum = underlyingPositionMap.get(underlyingConid).values().stream()
                .filter(pdh -> pdh.getInstrument().getRight() == Types.Right.Call)
                .mapToInt(PositionDataHolder::getPositionSize)
                .sum();

        udh.updatePositionsSum(putsSum, callsSum);
        messageService.sendWsMessage(udh, UnderlyingDataField.PUTS_SUM);
        messageService.sendWsMessage(udh, UnderlyingDataField.CALLS_SUM);
    }

    public void recalculateRiskDataPerUnderlying(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetRiskData();
            UnderlyingDataField.riskDataFields().forEach(field -> messageService.sendWsMessage(udh, field));

        } else if (udh.isRiskDataUpdateDue() && pdhs.stream().allMatch(PositionDataHolder::riskDataSourceFieldsReady)) {

            double delta = 0d, gamma = 0d, vega = 0d, theta = 0d, timeValue = 0d, callMargin = 0d, putMargin = 0d;

            for (PositionDataHolder pdh : pdhs) {
                int multiplier = pdh.getInstrument().getMultiplier();

                delta += pdh.getDelta() * pdh.getPositionSize() * multiplier;
                gamma += pdh.getGamma() * pdh.getPositionSize() * multiplier;
                vega += pdh.getVega() * pdh.getPositionSize() * multiplier;
                theta += pdh.getTheta() * pdh.getPositionSize() * multiplier;
                timeValue += pdh.getTimeValue() * Math.abs(pdh.getPositionSize()) * multiplier;

                if (pdh.getInstrument().getRight() == Types.Right.Call) {
                    callMargin += pdh.getMargin();
                } else if (pdh.getInstrument().getRight() == Types.Right.Put) {
                    putMargin += pdh.getMargin();
                }
            }
            double lastPrice = udh.getLast();
            double deltaOnePct = HopUtil.isValidPrice(lastPrice) ? (delta * lastPrice) / 100d : Double.NaN;
            double gammaOnePctPct = HopUtil.isValidPrice(lastPrice) ? (((gamma * lastPrice) / 100d) * lastPrice) / 100d : Double.NaN;
            double margin  = Math.max(callMargin, putMargin);
            double allocationPct = Double.NaN;

            if (accountSummary.isReady() && exchangeRates != null && exchangeRates.getBaseCurrency() == accountSummary.getBaseCurrency()) {
                Currency transactionCurrency = udh.getInstrument().getCurrency();
                double exchangeRate = exchangeRates.getRate(transactionCurrency);
                double netLiqValue = accountSummary.getNetLiquidationValue();
                allocationPct = 100d * margin / (netLiqValue * exchangeRate);
            }

            udh.updateRiskData(delta, deltaOnePct, gamma, gammaOnePctPct, vega, theta, timeValue, allocationPct);
            messageService.sendJmsMesage(HopSettings.JMS_DEST_RISK_DATA_RECALCULATED, underlyingConid);

            UnderlyingDataField.riskDataFields().forEach(field -> messageService.sendWsMessage(udh, field));
        }
    }

    public void recalculateUnrealizedPnlPerUnderlying(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetUnrealizedPnl();
        } else {
            double unrealizedPnl = underlyingPositionMap.get(underlyingConid).values().stream().mapToDouble(PositionDataHolder::getUnrealizedPnl).sum();
            udh.updateUnrealizedPnl(unrealizedPnl);
        }
        messageService.sendWsMessage(udh, UnderlyingDataField.UNREALIZED_PNL);
    }

    public void accountSummaryReceived(String account, String tag, String value, String currency) {
        accountSummary.update(account, tag, value, currency);
        messageService.sendWsMessage(WsTopic.ACCOUNT, accountSummary.getText());
    }

    public void historicalDataReceived(int requestId, Bar bar) {
        UnderlyingDataHolder udh = histDataRequestMap.get(requestId);

        LocalDate date = LocalDate.parse(bar.time(), HopSettings.IB_DATE_FORMATTER);
        double value = bar.close();
        udh.addImpliedVolatility(date, value);
    }

    public void historicalDataEndReceived(int requestId) {
        UnderlyingDataHolder udh = histDataRequestMap.get(requestId);
        udh.impliedVolatilityHistoryCompleted();

        udh.getIvHistoryDependentFields().forEach(field -> messageService.sendWsMessage(udh, field));
    }

    public UnderlyingDataHolder getUnderlyingDataHolder(int conid) {
        return underlyingMap.get(conid);
    }

    public UnderlyingMktDataSnapshot createMktDataSnapshot(int conid) {
        UnderlyingDataHolder udh = underlyingMap.get(conid);
        double price = Double.NaN;

        if (HopUtil.isValidPrice(udh.getLast())) {
            price = udh.getLast();
        } else if (HopUtil.isValidPrice(udh.getBid()) && HopUtil.isValidPrice(udh.getAsk())) {
            price = (udh.getBid() + udh.getAsk()) / 2d;
        } else if (HopUtil.isValidPrice(udh.getClose())) {
            price = udh.getClose();
        }

        return new UnderlyingMktDataSnapshot(price, udh.getOptionImpliedVol());
    }

    public String getAccountSummaryText() {
        return accountSummary.getText();
    }

    public List<UnderlyingDataHolder> getSortedUnderlyingDataHolders() {
        return underlyingMap.values().stream().sorted(Comparator
                .comparing(UnderlyingDataHolder::getDisplayRank)).collect(Collectors.toList());
    }
}
