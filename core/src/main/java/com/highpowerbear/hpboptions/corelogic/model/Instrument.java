package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.enums.InstrumentGroup;
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
    private final InstrumentGroup group;

    public Instrument(Types.SecType secType, String symbol, String underlying, Currency currency, Exchange exchange, InstrumentGroup group) {
        this.secType = secType;
        this.symbol = symbol;
        this.underlying = underlying;
        this.currency = currency;
        this.exchange = exchange;
        this.group = group;
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

    public InstrumentGroup getGroup() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instrument that = (Instrument) o;

        if (secType != that.secType) return false;
        if (!symbol.equals(that.symbol)) return false;
        if (!underlying.equals(that.underlying)) return false;
        if (currency != that.currency) return false;
        if (exchange != that.exchange) return false;
        return group == that.group;
    }

    @Override
    public int hashCode() {
        int result = secType.hashCode();
        result = 31 * result + symbol.hashCode();
        result = 31 * result + underlying.hashCode();
        result = 31 * result + currency.hashCode();
        result = 31 * result + exchange.hashCode();
        result = 31 * result + group.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "secType=" + secType +
                ", symbol='" + symbol + '\'' +
                ", underlying='" + underlying + '\'' +
                ", currency=" + currency +
                ", exchange=" + exchange +
                ", group=" + group +
                '}';
    }
}
