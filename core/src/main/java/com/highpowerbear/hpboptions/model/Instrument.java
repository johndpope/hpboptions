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
    private final String conId;
    private final Types.SecType secType;
    private final String symbol;
    private final String underlyingSymbol;
    private final Currency currency;
    private final Exchange exchange;
    private final Exchange primaryExchange;

    public Instrument(String conId, Types.SecType secType, String symbol, String underlyingSymbol, Currency currency, Exchange exchange, Exchange primaryExchange) {
        this.conId = conId;
        this.secType = secType;
        this.symbol = symbol;
        this.underlyingSymbol = underlyingSymbol;
        this.currency = currency;
        this.exchange = exchange;
        this.primaryExchange = primaryExchange;

        id = symbol.toLowerCase() + "-" + conId;
    }

    public Contract toIbContract() {
        Contract contract = new Contract();

        contract.localSymbol(symbol);
        contract.symbol(underlyingSymbol);
        contract.primaryExch(primaryExchange.name());
        contract.secType(secType);
        contract.exchange(exchange.name());
        contract.currency(currency.name());

        return contract;
    }

    public String getId() {
        return id;
    }

    public String getConId() {
        return conId;
    }

    public Types.SecType getSecType() {
        return secType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public Exchange getPrimaryExchange() {
        return primaryExchange;
    }
}
