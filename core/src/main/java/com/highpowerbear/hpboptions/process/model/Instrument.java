package com.highpowerbear.hpboptions.process.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Types;

/**
 * Created by robertk on 11/21/2018.
 */
public class Instrument {
    private Types.SecType secType;
    private String symbol;
    private String underlying;
    private Currency currency;
    private Exchange exchange;

    public Instrument(Types.SecType secType, String symbol, String underlying, Currency currency, Exchange exchange) {
        this.secType = secType;
        this.symbol = symbol;
        this.underlying = underlying;
        this.currency = currency;
        this.exchange = exchange;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Instrument that = (Instrument) o;

        if (secType != that.secType) return false;
        if (!symbol.equals(that.symbol)) return false;
        if (!underlying.equals(that.underlying)) return false;
        if (currency != that.currency) return false;
        return exchange == that.exchange;
    }

    @Override
    public int hashCode() {
        int result = secType.hashCode();
        result = 31 * result + symbol.hashCode();
        result = 31 * result + underlying.hashCode();
        result = 31 * result + currency.hashCode();
        result = 31 * result + exchange.hashCode();
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
                '}';
    }
}
