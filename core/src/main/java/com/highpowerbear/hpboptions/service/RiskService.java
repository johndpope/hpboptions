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
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class RiskService extends AbstractDataService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final AccountSummary accountSummary;
    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new HashMap<>(); // conid -> underlyingDataHolder
    private final Map<Integer, Map<Integer, PositionDataHolder>> underlyingPositionMap = new ConcurrentHashMap<>(); // underlying conid -> (position conid -> positionDataHolder)
    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder
    private final ReentrantLock positionLock = new ReentrantLock();

    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final Map<Integer, PositionDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> positionDataHolder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.RISK_IB_REQUEST_ID_INITIAL);

    @Value("${fixer.access-key}") private String fixerAccessKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private ExchangeRates exchangeRates;

    @Autowired
    public RiskService(IbController ibController, HopDao hopDao, MessageService messageService) {
        super(ibController, hopDao, messageService);

        ibController.addConnectionListener(this);
        accountSummary = new AccountSummary(ibRequestIdGen.incrementAndGet());
        initUnderlyings();
    }

    private void initUnderlyings() {
        List<Underlying> underlyings = hopDao.getActiveUnderlyings();

        for (Underlying underlying : underlyings) {
            int conid = underlying.getConid();

            Instrument instrument = new Instrument(conid, underlying.getSecType(), underlying.getSymbol(), underlying.getCurrency());
            instrument.setExchange(underlying.getExchange());
            instrument.setPrimaryExchange(underlying.getPrimaryExchange());

            UnderlyingDataHolder udh = new UnderlyingDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
            udh.setDisplayRank(underlying.getDisplayRank());

            underlyingMap.put(conid, udh);
            underlyingPositionMap.put(conid, new ConcurrentHashMap<>());
        }
    }

    @PostConstruct
    private void init() {
        retrieveExchangeRates();
    }

    @Override
    public void postConnect() {
        cancelAllMktData();
        cancelAllPnlSingle();

        underlyingMap.values().forEach(this::requestMktData);
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
        positionMap.values().forEach(this::requestMktData);
        positionMap.values().forEach(this::requestPnlSingle);
        ibController.requestPositions();
        ibController.requestAccountSummary(accountSummary.getIbRequestId(), accountSummary.getTags());
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();
        cancelAllPnlSingle();
        ibController.cancelPositions();
        ibController.cancelAccountSummary(accountSummary.getIbRequestId());
    }

    @Scheduled(cron="0 0 6 * * MON-FRI")
    private void performStartOfDayTasks() {
        retrieveExchangeRates();
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
        ibController.requestPositions();
        ibController.requestAccountSummary(accountSummary.getIbRequestId(), accountSummary.getTags());
    }

    private void cancelAllPnlSingle() {
        pnlRequestMap.keySet().forEach(ibController::cancelPnlSingle);
        pnlRequestMap.clear();
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

    private void requestPnlSingle(PositionDataHolder pdh) {
        int requestId = pdh.getIbPnlRequestId();

        pnlRequestMap.put(requestId, pdh);
        ibController.requestPnlSingle(requestId, pdh.getInstrument().getConid());
    }

    private void cancelPnlSingle(PositionDataHolder pdh) {
        int requestId = pdh.getIbPnlRequestId();

        ibController.cancelPnlSingle(requestId);
        pnlRequestMap.remove(requestId);
    }

    private void recalculateRiskDataPerUnderlying(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetRiskData();
            UnderlyingDataField.riskDataFields().forEach(field -> messageService.sendWsMessage(udh, field));

        } else if (udh.isRiskDataUpdateDue() && pdhs.stream().allMatch(AbstractOptionDataHolder::riskDataSourceFieldsReady)) {

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
            double deltaDollars = HopUtil.isValidPrice(lastPrice) ? delta * udh.getLast() : Double.NaN;
            double margin  = Math.max(callMargin, putMargin);
            double allocationPct = Double.NaN;

            if (accountSummary.isReady() && exchangeRates != null && exchangeRates.getBaseCurrency() == accountSummary.getBaseCurrency()) {
                Currency transactionCurrency = udh.getInstrument().getCurrency();
                double exchangeRate = exchangeRates.getRate(transactionCurrency);
                double netLiqValue = accountSummary.getNetLiquidationValue();
                allocationPct = margin / (netLiqValue * exchangeRate) * 100d;
            }

            udh.updateRiskData(delta, gamma, vega, theta, timeValue, deltaDollars, allocationPct);
            UnderlyingDataField.riskDataFields().forEach(field -> messageService.sendWsMessage(udh, field));
        }
    }

    private void recalculateUnrealizedPnlPerUnderlying(int underlyingConid) {
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

    private void retrieveExchangeRates() {
        String date = HopUtil.formatExchangeRateDate(LocalDate.now());
        String query = HopSettings.EXCHANGE_RATES_URL + "/" + date + "?access_key=" + fixerAccessKey + "&symbols=" + HopSettings.EXCHANGE_RATES_SYMBOLS;

        exchangeRates = restTemplate.getForObject(query, ExchangeRates.class);
        log.info("retrieved exchange rates " + exchangeRates);
    }

    @Override
    public void modelOptionDataReceived(OptionDataHolder optionDataHolder) {
        if (optionDataHolder.getType() == DataHolderType.POSITION) {
            PositionDataHolder pdh = (PositionDataHolder) optionDataHolder;
            pdh.recalculateMargin();
            messageService.sendWsMessage(pdh, PositionDataField.MARGIN);

            recalculateRiskDataPerUnderlying(optionDataHolder.getInstrument().getUnderlyingConid());
        }
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

    public void positionReceived(Contract contract, int positionSize) {
        positionLock.lock();
        try {
            int conid = contract.conid();
            PositionDataHolder pdh = positionMap.get(conid);

            if (pdh == null) {
                if (positionSize != 0) {
                    Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
                    String symbol = contract.localSymbol();
                    Currency currency = Currency.valueOf(contract.currency());
                    Types.Right right = contract.right();
                    double strike = contract.strike();
                    LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), HopSettings.IB_DATE_FORMATTER);
                    int multiplier = Integer.valueOf(contract.multiplier());
                    String underlyingSymbol = contract.symbol();

                    OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expiration, multiplier, underlyingSymbol);
                    pdh = new PositionDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
                    pdh.updatePositionSize(positionSize);
                    positionMap.put(conid, pdh);

                    ibController.requestContractDetails(ibRequestIdGen.incrementAndGet(), contract);
                }
            } else if (positionSize != 0) {
                if (positionSize != pdh.getPositionSize()) {
                    pdh.updatePositionSize(positionSize);
                    messageService.sendWsMessage(pdh, PositionDataField.POSITION_SIZE);

                    recalculateRiskDataPerUnderlying(pdh.getInstrument().getUnderlyingConid());
                }
            } else {
                cancelMktData(pdh);
                cancelPnlSingle(pdh);

                int underlyingConid = pdh.getInstrument().getUnderlyingConid();
                positionMap.remove(conid);
                underlyingPositionMap.get(underlyingConid).remove(conid);

                recalculateRiskDataPerUnderlying(underlyingConid);
                recalculateUnrealizedPnlPerUnderlying(underlyingConid);

                messageService.sendWsReloadRequestMessage(DataHolderType.POSITION);
            }
        } finally {
            positionLock.unlock();
        }
    }

    @Override
    public void contractDetailsReceived(int requestId, ContractDetails contractDetails) {
        Contract contract = contractDetails.contract();

        int conid = contract.conid();
        PositionDataHolder pdh = positionMap.get(conid);

        Exchange exchange = Exchange.valueOf(contract.exchange());
        double minTick = contractDetails.minTick();
        int underlyingConid = contractDetails.underConid();
        Types.SecType underlyingSecType = Types.SecType.valueOf(contractDetails.underSecType());

        OptionInstrument instrument = pdh.getInstrument();
        instrument.setExchange(exchange);
        instrument.setMinTick(minTick);
        instrument.setUnderlyingConid(underlyingConid);
        instrument.setUnderlyingSecType(underlyingSecType);

        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh != null) {
            pdh.setDisplayRank(udh.getDisplayRank());
            underlyingPositionMap.get(underlyingConid).put(conid, pdh);
        } else {
            pdh.setDisplayRank(-1);
        }

        messageService.sendWsReloadRequestMessage(DataHolderType.POSITION);
        requestMktData(pdh);
        requestPnlSingle(pdh);
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        PositionDataHolder pdh = pnlRequestMap.get(requestId);
        if (pdh == null) {
            return;
        }
        pdh.updateUnrealizedPnl(unrealizedPnL);
        messageService.sendWsMessage(pdh, PositionDataField.UNREALIZED_PNL);
        recalculateUnrealizedPnlPerUnderlying(pdh.getInstrument().getUnderlyingConid());
    }

    public PositionDataHolder getPositionDataHolder(int conid) {
        return positionMap.get(conid);
    }

    public double getUnderlyingPrice(int conid) {
        double bid = underlyingMap.get(conid).getBid();
        double ask = underlyingMap.get(conid).getAsk();
        double last = underlyingMap.get(conid).getLast();
        double close = underlyingMap.get(conid).getClose();

        if (HopUtil.isValidPrice(last)) {
            return last;
        } else if (HopUtil.isValidPrice(bid) && HopUtil.isValidPrice(ask)) {
            return (bid + ask) / 2d;
        } else if (HopUtil.isValidPrice(close)) {
            return close;
        } else {
            return Double.NaN;
        }
    }

    public double getUnderlyingOptionImpliedVol(int conid) {
        return underlyingMap.get(conid).getOptionImpliedVol();
    }

    public String getAccountSummaryText() {
        return accountSummary.getText();
    }

    public List<UnderlyingDataHolder> getSortedUnderlyingDataHolders() {
        return underlyingMap.values().stream().sorted(Comparator
                .comparing(UnderlyingDataHolder::getDisplayRank)).collect(Collectors.toList());
    }

    public List<PositionDataHolder> getSortedPositionDataHolders() {
        return positionMap.values().stream()
                .sorted(Comparator
                        .comparing(PositionDataHolder::getDaysToExpiration)
                        .thenComparing(PositionDataHolder::getDisplayRank)
                        .thenComparing(PositionDataHolder::getUnderlyingSymbol)
                        .thenComparing(PositionDataHolder::getRight)
                        .thenComparingDouble(PositionDataHolder::getStrike)).collect(Collectors.toList());
    }
}
