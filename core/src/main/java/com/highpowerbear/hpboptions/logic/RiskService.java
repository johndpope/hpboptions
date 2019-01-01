package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.common.MessageSender;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.model.*;
import com.ib.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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

    private final AccountSummary accountSummary;
    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new HashMap<>(); // conid -> underlyingDataHolder
    private final Map<Integer, Map<Integer, PositionDataHolder>> underlyingPositionMap = new ConcurrentHashMap<>(); // underlying conid -> (position conid -> positionDataHolder)
    private final Map<Integer, PositionDataHolder> positionMap = new ConcurrentHashMap<>(); // conid -> positionDataHolder
    private final ReentrantLock positionLock = new ReentrantLock();

    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final Map<Integer, PositionDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> positionDataHolder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(CoreSettings.IB_DATA_REQUEST_ID_INITIAL);

    @Autowired
    public RiskService(IbController ibController, CoreDao coreDao, MessageSender messageSender) {
        super(ibController, coreDao, messageSender);

        ibController.addConnectionListener(this);
        accountSummary = new AccountSummary(ibRequestIdGen.incrementAndGet());
        initUnderlyings();
    }

    private void initUnderlyings() {
        List<Underlying> underlyings = coreDao.getActiveUnderlyings();

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
                udh.createIbContract(),
                LocalDate.now().atStartOfDay().format(CoreSettings.IB_DATETIME_FORMATTER),
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

    private void recalculatePortfolioOptionData(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetPortfolioOptionData();
            UnderlyingDataField.portfolioFields().forEach(field -> messageSender.sendWsMessage(udh, field));

        } else if (udh.isPortfolioOptionDataUpdateDue() &&
                pdhs.stream().allMatch(AbstractOptionDataHolder::portfolioSourceFieldsReady)) {

            double delta = 0d, gamma = 0d, vega = 0d, theta = 0d, timeValue = 0d;

            for (PositionDataHolder pdh : pdhs) {
                int multiplier = pdh.getInstrument().getMultiplier();

                delta += pdh.getDelta() * pdh.getPositionSize() * multiplier;
                gamma += pdh.getGamma() * pdh.getPositionSize() * multiplier;
                vega += pdh.getVega() * pdh.getPositionSize() * multiplier;
                theta += pdh.getTheta() * pdh.getPositionSize() * multiplier;
                timeValue += pdh.getTimeValue() * Math.abs(pdh.getPositionSize()) * multiplier;
            }
            double lastPrice = udh.getLast();
            double deltaDollars = CoreUtil.isValidPrice(lastPrice) ? delta * udh.getLast() : Double.NaN;

            udh.updatePortfolioOptionData(delta, gamma, vega, theta, timeValue, deltaDollars);
            UnderlyingDataField.portfolioFields().forEach(field -> messageSender.sendWsMessage(udh, field));
        }
    }

    private void recalculatePortfolioPnl(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetPortfolioPnl();
        } else {
            double unrealizedPnl = underlyingPositionMap.get(underlyingConid).values().stream().mapToDouble(PositionDataHolder::getUnrealizedPnl).sum();
            udh.updatePortfolioPnl(unrealizedPnl);
        }
        messageSender.sendWsMessage(udh, UnderlyingDataField.UNREALIZED_PNL);
    }

    @Override
    public void modelOptionDataReceived(OptionDataHolder optionDataHolder) {
        if (optionDataHolder.getType() == DataHolderType.POSITION) {
            recalculatePortfolioOptionData(optionDataHolder.getInstrument().getUnderlyingConid());
        }
    }

    public void accountSummaryReceived(String account, String tag, String value, String currency) {
        accountSummary.update(account, tag, value, currency);
        messageSender.sendWsMessage(WsTopic.ACCOUNT, accountSummary.getText());
    }

    public void historicalDataReceived(int requestId, Bar bar) {
        UnderlyingDataHolder udh = histDataRequestMap.get(requestId);

        LocalDate date = LocalDate.parse(bar.time(), CoreSettings.IB_DATE_FORMATTER);
        double value = bar.close();
        udh.addImpliedVolatility(date, value);
    }

    public void historicalDataEndReceived(int requestId) {
        UnderlyingDataHolder udh = histDataRequestMap.get(requestId);
        udh.impliedVolatilityHistoryCompleted();

        udh.getIvHistoryDependentFields().forEach(field -> messageSender.sendWsMessage(udh, field));
    }

    public void positionReceived(Contract contract, int positionSize) {
        positionLock.lock();
        try {
            int conid = contract.conid();
            PositionDataHolder pdh = positionMap.get(conid);

            if (pdh == null) {
                if (positionSize != 0) {
                    Types.SecType secType = Types.SecType.valueOf(contract.getSecType());
                    String underlyingSymbol = contract.symbol();
                    String symbol = contract.localSymbol();
                    Currency currency = Currency.valueOf(contract.currency());
                    int multiplier = Integer.valueOf(contract.multiplier());

                    Types.Right right = contract.right();
                    double strike = contract.strike();
                    LocalDate expiration = LocalDate.parse(contract.lastTradeDateOrContractMonth(), CoreSettings.IB_DATE_FORMATTER);

                    OptionInstrument instrument = new OptionInstrument(conid, secType, symbol, currency, right, strike, expiration, multiplier, underlyingSymbol);
                    pdh = new PositionDataHolder(instrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet());
                    pdh.updatePositionSize(positionSize);
                    positionMap.put(conid, pdh);

                    ibController.requestContractDetails(ibRequestIdGen.incrementAndGet(), contract);
                }
            } else if (positionSize != 0) {
                if (positionSize != pdh.getPositionSize()) {
                    pdh.updatePositionSize(positionSize);
                    messageSender.sendWsMessage(pdh, PositionDataField.POSITION_SIZE);

                    recalculatePortfolioOptionData(pdh.getInstrument().getUnderlyingConid());
                }
            } else {
                cancelMktData(pdh);
                cancelPnlSingle(pdh);

                int underlyingConid = pdh.getInstrument().getUnderlyingConid();
                positionMap.remove(conid);
                underlyingPositionMap.get(underlyingConid).remove(conid);

                recalculatePortfolioOptionData(underlyingConid);
                recalculatePortfolioPnl(underlyingConid);

                messageSender.sendWsReloadRequestMessage(DataHolderType.POSITION);
            }
        } finally {
            positionLock.unlock();
        }
    }

    @Override
    public void contractDetailsReceived(ContractDetails contractDetails) {
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

        messageSender.sendWsReloadRequestMessage(DataHolderType.POSITION);
        requestMktData(pdh);
        requestPnlSingle(pdh);
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        PositionDataHolder pdh = pnlRequestMap.get(requestId);
        if (pdh == null) {
            return;
        }
        pdh.updateUnrealizedPnl(unrealizedPnL);
        messageSender.sendWsMessage(pdh, PositionDataField.UNREALIZED_PNL);
        recalculatePortfolioPnl(pdh.getInstrument().getUnderlyingConid());
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

    public double getUnderlyingPrice(int conid) {
        double bid = underlyingMap.get(conid).getBid();
        double ask = underlyingMap.get(conid).getAsk();
        double last = underlyingMap.get(conid).getLast();
        double close = underlyingMap.get(conid).getClose();

        if (CoreUtil.isValidPrice(last)) {
            return last;
        } else if (CoreUtil.isValidPrice(bid) && CoreUtil.isValidPrice(ask)) {
            return (bid + ask) / 2d;
        } else if (CoreUtil.isValidPrice(close)) {
            return close;
        } else {
            return Double.NaN;
        }
    }

    public double getUnderlyingOptionImpliedVol(int conid) {
        return underlyingMap.get(conid).getOptionImpliedVol();
    }
}
