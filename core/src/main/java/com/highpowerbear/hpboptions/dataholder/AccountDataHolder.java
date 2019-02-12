package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.Currency;
import com.ib.controller.AccountSummaryTag;

/**
 * Created by robertk on 2/12/2019.
 */
public class AccountDataHolder {

    private final String ibAccount;
    private final int ibPnlRequestId;

    private Currency baseCurrency;
    private Double netLiquidationValue;
    private Double availableFunds;
    private Double unrealizedPnl;

    public AccountDataHolder(String ibAccount, int ibPnlRequestId) {
        this.ibAccount = ibAccount;
        this.ibPnlRequestId = ibPnlRequestId;
    }

    public boolean isReady() {
        return baseCurrency != null && netLiquidationValue != null && availableFunds != null && unrealizedPnl != null;
    }

    public void updateAccountSummary(AccountSummaryTag tag, double value) {
        if (tag == AccountSummaryTag.NetLiquidation) {
            setNetLiquidationValue(value);

        } else if (tag == AccountSummaryTag.AvailableFunds) {
            setAvailableFunds(value);
        }
    }

    public String getIbAccount() {
        return ibAccount;
    }

    public int getIbPnlRequestId() {
        return ibPnlRequestId;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(Currency baseCurrency) {
        if (this.baseCurrency == null) {
            this.baseCurrency = baseCurrency;

        } else if (this.baseCurrency != baseCurrency) {
            throw new IllegalStateException("base currency cannot be changed, existing=" + this.baseCurrency + ", new=" + baseCurrency);
        }
    }

    public Double getNetLiquidationValue() {
        return netLiquidationValue;
    }

    public void setNetLiquidationValue(double netLiquidationValue) {
        this.netLiquidationValue = netLiquidationValue;
    }

    public Double getAvailableFunds() {
        return availableFunds;
    }

    public void setAvailableFunds(double availableFunds) {
        this.availableFunds = availableFunds;
    }

    public Double getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(double unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }
}
