package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.ib.controller.AccountSummaryTag;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.ib.controller.AccountSummaryTag.AvailableFunds;
import static com.ib.controller.AccountSummaryTag.NetLiquidation;

/**
 * Created by robertk on 12/19/2018.
 */
public class AccountSummary {

    private final int ibRequestId;
    private Set<AccountSummaryTag> tags = new LinkedHashSet<>();

    private String account;
    private Currency baseCurrency;
    private Map<AccountSummaryTag, Double> summaryMap = new HashMap<>(); // tag -> value
    private String text = "Account summary N/A";

    public AccountSummary(int ibRequestId) {
        this.ibRequestId = ibRequestId;
        tags.add(NetLiquidation);
        tags.add(AvailableFunds);
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public void update(String account, String tag, String value, String currency) {
        if (this.account == null) {
            this.account = account;
        } else if (!this.account.equals(account)) {
            throw new IllegalStateException("received account summary update for account " + account);
        }

        if (baseCurrency == null) {
            baseCurrency = Currency.valueOf(currency);
        } else if (baseCurrency != Currency.valueOf(currency)) {
            throw new IllegalStateException("received account summary update for currency " + currency);
        }

        AccountSummaryTag summaryTag = AccountSummaryTag.valueOf(tag);
        summaryMap.put(summaryTag, Double.valueOf(value));

        updateText();
    }

    public String getTags() {
        return StringUtils.join(tags, ",");
    }

    public String getText() {
        return text;
    }

    public Double getNetLiquidationValue() {
        return summaryMap.get(NetLiquidation);
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public boolean isReady() {
        return account != null && baseCurrency != null && getNetLiquidationValue() != null;
    }

    private void updateText() {
        StringBuilder sb = new StringBuilder();
        sb.append(account).append(": ");

        for (AccountSummaryTag tag : tags) {
            if (summaryMap.get(tag) != null) {
                sb.append(tag.name()).append(" ").append(Math.round(summaryMap.get(tag))).append(" ").append(baseCurrency.name()).append(", ");
            }
        }
        text = sb.toString().replaceAll(", $", "");
    }
}
