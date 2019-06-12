package com.highpowerbear.hpboptions.dataholder;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.AccountDataField;
import com.ib.controller.AccountSummaryTag;

/**
 * Created by robertk on 2/12/2019.
 */
public class AccountDataHolder extends AbstractDataHolder {

    private final String ibAccount;
    private final int ibPnlRequestId;
    private Currency baseCurrency;

    public AccountDataHolder(String ibAccount, int ibPnlRequestId) {
        super(DataHolderType.ACCOUNT);
        this.ibAccount = ibAccount;
        this.ibPnlRequestId = ibPnlRequestId;

        id = type.name().toLowerCase() + "-" + ibAccount;

        AccountDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));
    }

    public boolean isReady() {
        return baseCurrency != null && !Double.isNaN(getNetLiquidationValue()) && !Double.isNaN(getAvailableFunds()) && !Double.isNaN(getUnrealizedPnl());
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

    public void updateAccountSummary(AccountSummaryTag tag, double value) {
        if (tag == AccountSummaryTag.NetLiquidation) {
            updateNetLiquidationValue(value);

        } else if (tag == AccountSummaryTag.AvailableFunds) {
            updateAvailableFunds(value);
        }
    }

    public double getNetLiquidationValue() {
        return getCurrent(AccountDataField.NET_LIQUIDATION_VALUE).doubleValue();
    }

    public void updateNetLiquidationValue(double netLiquidationValue) {
        update(AccountDataField.NET_LIQUIDATION_VALUE, netLiquidationValue);
    }

    public double getAvailableFunds() {
        return getCurrent(AccountDataField.AVAILABLE_FUNDS).doubleValue();
    }

    public void updateAvailableFunds(double availableFunds) {
        update(AccountDataField.AVAILABLE_FUNDS, availableFunds);
    }

    public double getUnrealizedPnl() {
        return getCurrent(AccountDataField.UNREALIZED_PNL).doubleValue();
    }

    public void updateUnrealizedPnl(double unrealizedPnl) {
        update(AccountDataField.UNREALIZED_PNL, unrealizedPnl);
    }
}
