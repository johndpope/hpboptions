package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.common.HopUtil;
import com.highpowerbear.hpboptions.connector.ConnectionListener;
import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.dataholder.AccountDataHolder;
import com.highpowerbear.hpboptions.model.ExchangeRates;
import com.ib.controller.AccountSummaryTag;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ib.controller.AccountSummaryTag.AvailableFunds;
import static com.ib.controller.AccountSummaryTag.NetLiquidation;

/**
 * Created by robertk on 2/12/2019.
 */
@Service
public class AccountService implements ConnectionListener {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final IbController ibController;
    private final MessageService messageService;

    private final Map<String, AccountDataHolder> accountMap = new HashMap<>(); // ibAccount -> account data holder (should be only 1)
    private final Map<Integer, AccountDataHolder> pnlRequestMap = new HashMap<>(); // ib request id -> account data holder

    private final AtomicInteger ibRequestIdGen = new AtomicInteger(HopSettings.ACCOUNT_IB_REQUEST_ID_INITIAL);
    private final int ibAccountSummaryRequestId;
    private final Set<AccountSummaryTag> accountSummaryTags;

    @Value("${fixer.access-key}")
    private String fixerAccessKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private ExchangeRates exchangeRates;

    @Autowired
    public AccountService(IbController ibController, MessageService messageService) {
        this.ibController = ibController;
        this.messageService = messageService;

        ibAccountSummaryRequestId = ibRequestIdGen.incrementAndGet();
        accountSummaryTags = Stream.of(NetLiquidation, AvailableFunds).collect(Collectors.toSet());

        ibController.addConnectionListener(this);
    }

    @PostConstruct
    private void init() {
        retrieveExchangeRates();
    }

    @Override
    public void preConnect() {
    }

    @Override
    public void postConnect() {
        ibController.requestAccountSummary(ibAccountSummaryRequestId, accountSummaryTagsCsv());
        cancelAllPnl();
        accountMap.values().forEach(this::requestPnl);
    }

    @Override
    public void preDisconnect() {
        ibController.cancelAccountSummary(ibAccountSummaryRequestId);
        cancelAllPnl();
    }

    @Override
    public void postDisconnect() {
    }

    public boolean isReady(String account) {
        AccountDataHolder adh = accountMap.get(account);
        return adh != null && adh.isReady() && exchangeRates != null && exchangeRates.getBaseCurrency() == adh.getBaseCurrency();
    }

    @Scheduled(cron = "0 0 7 * * MON-FRI")
    private void performStartOfDayTasks() {
        retrieveExchangeRates();
        ibController.requestAccountSummary(ibAccountSummaryRequestId, accountSummaryTagsCsv());
    }

    private void requestPnl(AccountDataHolder adh) {
        int requestId = adh.getIbPnlRequestId();

        pnlRequestMap.put(requestId, adh);
        ibController.requestPnl(requestId, adh.getIbAccount());
    }

    private void cancelAllPnl() {
        pnlRequestMap.keySet().forEach(ibController::cancelPnl);
        pnlRequestMap.clear();
    }

    private String accountSummaryTagsCsv() {
        return StringUtils.join(accountSummaryTags, ",");
    }

    private void retrieveExchangeRates() {
        String date = HopUtil.formatExchangeRateDate(LocalDate.now());
        String query = HopSettings.EXCHANGE_RATES_URL + "/" + date + "?access_key=" + fixerAccessKey + "&symbols=" + HopSettings.EXCHANGE_RATES_SYMBOLS;

        exchangeRates = restTemplate.getForObject(query, ExchangeRates.class);
        log.info("retrieved exchange rates " + exchangeRates);
    }

    public void accountSummaryReceived(String account, String tag, String value, String currency) {
        AccountDataHolder adh = accountMap.get(account);

        if (adh == null) {
            adh = new AccountDataHolder(account, ibRequestIdGen.incrementAndGet());
            accountMap.put(account, adh);
            requestPnl(adh);
        }
        adh.setBaseCurrency(Currency.valueOf(currency));
        adh.updateAccountSummary(AccountSummaryTag.valueOf(tag), HopUtil.round4(Double.valueOf(value)));

        messageService.sendWsReloadRequestMessage(DataHolderType.ACCOUNT);
    }

    public void unrealizedPnlReceived(int requestId, double unrealizedPnL) {
        AccountDataHolder adh = pnlRequestMap.get(requestId);

        if (adh != null) {
            adh.setUnrealizedPnl(HopUtil.round4(unrealizedPnL));
            messageService.sendWsReloadRequestMessage(DataHolderType.ACCOUNT);
        }
    }

    public double getExchangeRate(Currency transactionCurrency) {
        return exchangeRates.getRate(transactionCurrency);
    }

    public double getNetLiquidationValue(String ibAccount) {
        return accountMap.get(ibAccount).getNetLiquidationValue();
    }

    public Collection<AccountDataHolder> getAccountDataHolders() {
        return accountMap.values();
    }
}
