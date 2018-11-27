package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Contract;
import com.ib.client.Types;

/**
 * Created by robertk on 11/21/2018.
 */
public class Instrument {

    private final Types.SecType secType;
    private final String symbol;
    private final String underlying;
    private final Currency currency;
    private final Exchange exchange;

    public Instrument(Types.SecType secType, String symbol, String underlying, Currency currency, Exchange exchange) {
        this.secType = secType;
        this.symbol = symbol;
        this.underlying = underlying;
        this.currency = currency;
        this.exchange = exchange;
    }

    public Contract toIbContract() {
        Contract contract = new Contract();

        contract.localSymbol(symbol);
        contract.symbol(underlying);
        contract.secType(secType);
        contract.exchange(exchange.name());
        contract.currency(currency.name());

        return contract;
    }

    public Types.SecType getSecType() {
        return secType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getUnderlying() {
        return underlying;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public String toString() {
        return secType + "-" + symbol + "-" + underlying + "-" + currency + "-" + exchange;
    }
}
