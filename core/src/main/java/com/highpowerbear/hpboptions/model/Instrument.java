package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Contract;
import com.ib.client.Types;

/**
 * Created by robertk on 12/18/2018.
 */
public class Instrument {

    protected final int conid;
    private final Types.SecType secType;
    private final String underlyingSymbol;
    protected final String symbol;
    protected final Currency currency;
    private Types.SecType underlyingSecType;
    private Integer underlyingConid;
    private Exchange exchange;
    private Exchange primaryExchange;
    private Double minTick;

    public Instrument(int conid, Types.SecType secType, String underlyingSymbol, String symbol, Currency currency) {
        this.conid = conid;
        this.secType = secType;
        this.underlyingSymbol = underlyingSymbol;
        this.symbol = symbol;
        this.currency = currency;
    }

    public Contract createIbContract() {
        Contract contract = new Contract();

        contract.conid(conid);
        contract.secType(secType);

        if (underlyingSymbol != null) {
            contract.symbol(underlyingSymbol);
        }
        contract.localSymbol(symbol);
        contract.currency(currency.name());
        contract.exchange(exchange.getCode());

        if (primaryExchange != null) {
            contract.primaryExch(primaryExchange.name());
        }
        return contract;
    }

    public int getConid() {
        return conid;
    }

    public Types.SecType getSecType() {
        return secType;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Types.SecType getUnderlyingSecType() {
        return underlyingSecType;
    }

    public void setUnderlyingSecType(Types.SecType underlyingSecType) {
        this.underlyingSecType = underlyingSecType;
    }

    public Integer getUnderlyingConid() {
        return underlyingConid;
    }

    public void setUnderlyingConid(Integer underlyingConid) {
        this.underlyingConid = underlyingConid;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public Exchange getPrimaryExchange() {
        return primaryExchange;
    }

    public void setPrimaryExchange(Exchange primaryExchange) {
        this.primaryExchange = primaryExchange;
    }

    public Double getMinTick() {
        return minTick;
    }

    public void setMinTick(Double minTick) {
        this.minTick = minTick;
    }

    @Override
    public String toString() {
        return conid + ", " + secType + ", " + symbol + ", " + currency + ", " + exchange;
    }
}
