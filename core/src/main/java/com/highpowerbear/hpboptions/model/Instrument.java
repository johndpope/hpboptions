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
    protected final Types.SecType secType;
    protected final String symbol;
    protected final Currency currency;
    protected Exchange exchange;
    protected Exchange primaryExchange;

    public Instrument(int conid, Types.SecType secType, String symbol, Currency currency) {
        this.conid = conid;
        this.secType = secType;
        this.symbol = symbol;
        this.currency = currency;
    }

    public Contract createIbContract() {
        Contract contract = new Contract();
        contract.conid(conid);
        contract.secType(secType);
        contract.localSymbol(symbol);
        contract.currency(currency.name());
        contract.exchange(exchange.name());

        return contract;
    }

    public int getConid() {
        return conid;
    }

    public Types.SecType getSecType() {
        return secType;
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

    @Override
    public String toString() {
        return conid + ", " + secType + ", " + symbol + ", " + currency + ", " + exchange;
    }
}
