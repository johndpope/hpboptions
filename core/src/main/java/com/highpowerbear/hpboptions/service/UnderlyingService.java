package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.database.HopDao;
import com.highpowerbear.hpboptions.database.Underlying;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.*;
import com.highpowerbear.hpboptions.field.UnderlyingDataField;
import com.highpowerbear.hpboptions.model.Instrument;
import com.highpowerbear.hpboptions.dataholder.PositionDataHolder;
import com.highpowerbear.hpboptions.dataholder.UnderlyingDataHolder;
import com.highpowerbear.hpboptions.model.UnderlyingInfo;
import com.highpowerbear.hpboptions.model.UnderlyingMktDataSnapshot;
import com.ib.client.Bar;
import com.ib.client.Contract;
import com.ib.client.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by robertk on 11/5/2018.
 */
@Service
public class UnderlyingService extends AbstractMarketDataService implements ConnectionListener {

    private final HopDao hopDao;
    private final AccountService accountService;

    private final Map<Integer, Underlying> underlyingEntityMap = new HashMap<>(); // conid -> underlying
    private final List<UnderlyingInfo> underlyingInfos = new ArrayList<>();

    private final Map<Integer, UnderlyingDataHolder> underlyingMap = new HashMap<>(); // conid -> underlyingDataHolder
    private final Map<Integer, UnderlyingDataHolder> underlyingCfdMap = new HashMap<>(); // cfd conid -> underlyingDataHolder
    private final Map<Integer, UnderlyingDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final Map<Integer, Map<Integer, PositionDataHolder>> underlyingPositionMap = new ConcurrentHashMap<>(); // underlying conid -> (position conid -> positionDataHolder)

    private final Map<Integer, UnderlyingDataHolder> histDataRequestMap = new HashMap<>(); // ib request id -> underlyingDataHolder
    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.UNDERLYING_IB_REQUEST_ID_INITIAL);

    @Value("${ib.account}")
    private String ibAccount;

    @Autowired
    public UnderlyingService(IbController ibController, MessageService messageService, HopDao hopDao, AccountService accountService) {
        super(ibController, messageService);
        this.hopDao = hopDao;
        this.accountService = accountService;

        ibController.addConnectionListener(this);
        init();
    }

    private void init() {
        for (Underlying u : hopDao.getActiveUnderlyings()) {
            int conid = u.getConid();

            underlyingEntityMap.put(conid, u);
            underlyingInfos.add(new UnderlyingInfo(u.getConid(), u.getSymbol()));

            Instrument instrument = new Instrument(conid, u.getSecType(),null, u.getSymbol(), u.getCurrency());
            instrument.setExchange(u.getExchange());
            instrument.setPrimaryExchange(u.getPrimaryExchange());

            Instrument cfdInstrument = null;
            if (u.isCfdDefined()) {
                cfdInstrument = new Instrument(u.getCfdConid(), Types.SecType.CFD, u.getSymbol(), u.getCfdSymbol(), u.getCurrency());
                cfdInstrument.setUnderlyingSecType(u.getSecType());
                cfdInstrument.setUnderlyingConid(u.getConid());
                cfdInstrument.setExchange(u.getExchange());
                cfdInstrument.setMinTick(u.getCfdMinTick());
            }

            UnderlyingDataHolder udh = new UnderlyingDataHolder(instrument, cfdInstrument, ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet(), ibRequestIdGen.incrementAndGet(), u.getMarketOpen(), u.getMarketClose());
            if (u.isCfdDefined()) {
                underlyingCfdMap.put(u.getCfdConid(), udh);
            }
            udh.setDisplayRank(u.getDisplayRank());

            underlyingMap.put(conid, udh);
            underlyingPositionMap.put(conid, new ConcurrentHashMap<>());
        }
    }

    @Override
    public void postConnect() {
        cancelAllMktData();

        underlyingMap.values().forEach(this::requestMktData);
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);

        underlyingCfdMap.values().forEach(udh -> {
            pnlRequestMap.put(udh.getIbPnlRequestId(), udh);
            ibController.requestPnlSingle(udh.getIbPnlRequestId(), ibAccount, udh.getCfdInstrument().getConid());
        });
    }

    @Override
    public void preDisconnect() {
        cancelAllMktData();

        pnlRequestMap.keySet().forEach(ibController::cancelPnlSingle);
        pnlRequestMap.clear();
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        underlyingMap.values().forEach(this::requestImpliedVolatilityHistory);
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

    public void addOptionPosition(int underlyingConid, PositionDataHolder pdh) {
        if (underlyingMap.containsKey(underlyingConid)) {
            underlyingPositionMap.get(underlyingConid).put(pdh.getInstrument().getConid(), pdh);
            calculateOptionPositionsSum(underlyingConid);
        }
    }

    public void removeOptionPosition(int underlyingConid, int positionConid) {
        if (underlyingMap.containsKey(underlyingConid)) {
            underlyingPositionMap.get(underlyingConid).remove(positionConid);
            calculateOptionPositionsSum(underlyingConid);
        }
    }

    public void calculateOptionPositionsSum(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null) {
            return;
        }
        Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

        if (pdhs.isEmpty()) {
            udh.resetOptionPositionsSum();
        } else {
            int putsShort = optionPositionsSumPerType(underlyingConid, Types.Right.Put, -1);
            int putsLong = optionPositionsSumPerType(underlyingConid, Types.Right.Put, 1);
            int callsShort = optionPositionsSumPerType(underlyingConid, Types.Right.Call, -1);
            int callsLong = optionPositionsSumPerType(underlyingConid, Types.Right.Call, 1);

            udh.updateOptionPositionsSum(putsShort, putsLong, callsShort, callsLong);
        }
        UnderlyingDataField.optionPositionSumFields().forEach(field -> messageService.sendWsMessage(udh, field));
    }

    private int optionPositionsSumPerType(int underlyingConid, Types.Right right, int sign) {
        return underlyingPositionMap.get(underlyingConid).values().stream()
                .filter(pdh -> pdh.getInstrument().getRight() == right)
                .mapToInt(PositionDataHolder::getPositionSize)
                .filter(pos -> sign == -1 ? pos < 0 : pos > 0)
                .sum();
    }

    public void calculateRiskDataPerUnderlying(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null || !udh.getRiskCalculationLock().tryLock()) {
            return;
        }
        try {
            Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

            if (pdhs.isEmpty()) {
                udh.resetRiskData();

                if (udh.getCfdPositionSize() != 0) {
                    double delta = udh.getCfdPositionSize();
                    double deltaOnePct = calculateDeltaOnePct(delta, udh.getLast());
                    double allocationPct = calculateAllocationPct(udh.getCfdMargin(), udh.getInstrument().getCurrency());

                    udh.updateCfdOnlyRiskData(delta, deltaOnePct, udh.getCfdMargin(), allocationPct);
                }
                sendRiskMessages(udh);

            } else if (pdhs.stream().allMatch(PositionDataHolder::riskDataSourceFieldsReady)) {
                double delta = udh.getCfdPositionSize();
                double gamma = 0d, vega = 0d, theta = 0d, timeValue = 0d, callMargin = 0d, putMargin = 0d;

                for (PositionDataHolder pdh : pdhs) {
                    int factor = pdh.getPositionSize() * pdh.getInstrument().getMultiplier();

                    delta += pdh.getDelta() * factor;
                    gamma += pdh.getGamma() * factor;
                    vega += pdh.getVega() * factor;
                    theta += pdh.getTheta() * factor;

                    timeValue += pdh.getTimeValue() * -pdh.getPositionSize() * pdh.getInstrument().getMultiplier();

                    callMargin += pdh.getInstrument().isCall() ? pdh.getMargin() : 0d;
                    putMargin += pdh.getInstrument().isPut() ? pdh.getMargin() : 0d;
                }
                double deltaOnePct = calculateDeltaOnePct(delta, udh.getLast());
                double gammaOnePctPct = calculateGammaOnePct(gamma, udh.getLast());

                callMargin += udh.getCfdPositionSize() < 0 ? udh.getCfdMargin() : 0d;
                putMargin += udh.getCfdPositionSize() > 0 ? udh.getCfdMargin() : 0d;

                double margin = Math.max(callMargin, putMargin);
                double allocationPct = calculateAllocationPct(margin, udh.getInstrument().getCurrency());

                udh.updateRiskData(delta, deltaOnePct, gamma, gammaOnePctPct, vega, theta, timeValue, margin, allocationPct);
                sendRiskMessages(udh);
            }
        } finally {
          udh.getRiskCalculationLock().unlock();
        }
    }

    private void sendRiskMessages(UnderlyingDataHolder udh) {
        messageService.sendJmsMesage(HopSettings.JMS_DEST_UNDERLYING_RISK_DATA_CALCULATED, udh.getInstrument().getConid());
        UnderlyingDataField.riskDataFields().forEach(field -> messageService.sendWsMessage(udh, field));
    }

    private double calculateDeltaOnePct(double delta, double price) {
        return HopUtil.isValidPrice(price) ? (delta * price) / 100d : Double.NaN;
    }

    private double calculateGammaOnePct(double gamma, double price) {
        return HopUtil.isValidPrice(price) ? (((gamma * price) / 100d) * price) / 100d : Double.NaN;
    }

    private double calculateAllocationPct(double margin, Currency currency) {
        double allocationPct = Double.NaN;

        if (accountService.isReady(ibAccount)) {
            double exchangeRate = accountService.getExchangeRate(currency);
            double netLiqValue = accountService.getNetLiquidationValue(ibAccount);
            allocationPct = 100d * margin / (netLiqValue * exchangeRate);
        }
        return allocationPct;
    }

    public void calculateUnrealizedPnlPerUnderlying(int underlyingConid) {
        UnderlyingDataHolder udh = underlyingMap.get(underlyingConid);
        if (udh == null || !udh.getPnlCalculationLock().tryLock()) {
            return;
        }
        try {
            Collection<PositionDataHolder> pdhs = underlyingPositionMap.get(underlyingConid).values();

            if (pdhs.isEmpty()) {
                if (udh.getCfdPositionSize() == 0) {
                    udh.resetPortfolioUnrealizedPnl();
                } else {
                    udh.updatePortfolioUnrealizedPnl(udh.getCfdUnrealizedPnl());
                }
            } else {
                double unrealizedPnl = underlyingPositionMap.get(underlyingConid).values().stream().mapToDouble(PositionDataHolder::getUnrealizedPnl).sum();
                if (udh.getCfdPositionSize() != 0) {
                    unrealizedPnl += udh.getCfdUnrealizedPnl();
                }
                udh.updatePortfolioUnrealizedPnl(unrealizedPnl);
            }
            messageService.sendWsMessage(udh, UnderlyingDataField.PORTFOLIO_UNREALIZED_PNL);
        } finally {
            udh.getPnlCalculationLock().unlock();
        }
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
        int conid = contract.conid();
        UnderlyingDataHolder udh = underlyingCfdMap.get(conid);

        if (udh != null) {
            udh.updateCfdPositionSize(positionSize);
            messageService.sendWsMessage(udh, UnderlyingDataField.CFD_POSITION_SIZE);

            if (positionSize == 0) {
                udh.resetCfdFields();
                UnderlyingDataField.cfdFields().forEach(field -> messageService.sendWsMessage(udh, field));
                calculateUnrealizedPnlPerUnderlying(udh.getInstrument().getConid());
            }
            calculateRiskDataPerUnderlying(udh.getInstrument().getConid());
        }
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        UnderlyingDataHolder udh = pnlRequestMap.get(requestId);

        if (udh != null) {
            if (udh.getCfdPositionSize() != 0) {
                udh.updateCfdUnrealizedPnl(unrealizedPnL);
                messageService.sendWsMessage(udh, UnderlyingDataField.CFD_UNREALIZED_PNL);
            }
            calculateUnrealizedPnlPerUnderlying(udh.getInstrument().getConid());
        }
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

    public Underlying getUnderlying(int conid) {
        return underlyingEntityMap.get(conid);
    }

    public UnderlyingDataHolder getUnderlyingDataHolder(int conid) {
        return underlyingMap.get(conid);
    }

    public List<UnderlyingInfo> getUnderlyingInfos() {
        return underlyingInfos;
    }

    public List<UnderlyingDataHolder> getSortedUnderlyingDataHolders() {
        return underlyingMap.values().stream().sorted(Comparator
                .comparing(UnderlyingDataHolder::getDisplayRank)).collect(Collectors.toList());
    }
}
