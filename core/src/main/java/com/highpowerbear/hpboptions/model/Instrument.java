package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Contract;
import com.ib.client.Types;

/**
 * Created by robertk on 11/21/2018.
 */
public class Instrument {
    private final String id;

    private final int conid;

    private final Types.SecType secType;
    private final String underlyingSymbol;
    private final String symbol;
    private final Currency currency;

    private Exchange exchange;
    private Exchange primaryExchange;

    private Integer underlyingConid;
    private Types.SecType underlyingSecType;

    public Instrument(int conid, Types.SecType secType, String underlyingSymbol, String symbol, Currency currency, Exchange exchange, Exchange primaryExchange) {
        this.conid = conid;
        this.secType = secType;
        this.underlyingSymbol = underlyingSymbol;
        this.symbol = symbol;
        this.currency = currency;
        this.exchange = exchange;
        this.primaryExchange = primaryExchange;

        id = symbol.toLowerCase().replaceAll("\\s+","") + "-" + conid;
    }

    public Contract toIbContract() {
        Contract contract = new Contract();

        contract.conid(conid);
        contract.secType(secType);
        contract.localSymbol(symbol);
        contract.currency(currency.name());

        contract.exchange(exchange != null ? exchange.name() : null);
        contract.primaryExch(primaryExchange != null ? primaryExchange.name() : null);
        contract.symbol(underlyingSymbol);

        return contract;
    }

    public String getId() {
        return id;
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

    public Integer getUnderlyingConid() {
        return underlyingConid;
    }

    public void setUnderlyingConid(Integer underlyingConid) {
        this.underlyingConid = underlyingConid;
    }

    public Types.SecType getUnderlyingSecType() {
        return underlyingSecType;
    }

    public void setUnderlyingSecType(Types.SecType underlyingSecType) {
        this.underlyingSecType = underlyingSecType;
    }
}
