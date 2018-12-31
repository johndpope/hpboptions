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
    private Map<String, Map<AccountSummaryTag, Entry>> summaryMap = new HashMap<>(); // account -> (tag -> (value, currency))
    private String text = "Account summary N/A";

    private class Entry {
        private double value;
        private Currency currency;

        private Entry(double value, Currency currency) {
            this.value = value;
            this.currency = currency;
        }
    }

    public AccountSummary(int ibRequestId) {
        this.ibRequestId = ibRequestId;
        tags.add(NetLiquidation);
        tags.add(AvailableFunds);
    }

    public int getIbRequestId() {
        return ibRequestId;
    }

    public void update(String account, String tag, String value, String currency) {
        AccountSummaryTag summaryTag = AccountSummaryTag.valueOf(tag);

        summaryMap.putIfAbsent(account, new HashMap<>());
        summaryMap.get(account).put(summaryTag, new Entry(Double.valueOf(value), Currency.valueOf(currency)));

        updateText();
    }

    public String getTags() {
        return StringUtils.join(tags, ",");
    }

    public String getText() {
        return text;
    }

    private void updateText() {
        StringBuilder sb = new StringBuilder();

        for (String account : summaryMap.keySet()) {
            sb.append(account).append(": ");

            Map<AccountSummaryTag, Entry> map = summaryMap.get(account);

            for (AccountSummaryTag tag : tags) {
                Entry entry = map.get(tag);
                if (entry != null) {
                    sb.append(tag.name()).append(" ").append(Math.round(entry.value)).append(" ").append(entry.currency.name()).append(", ");
                }
            }
        }
        text = sb.toString().replaceAll(", $", "");
    }
}
