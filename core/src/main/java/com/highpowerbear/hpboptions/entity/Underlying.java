package com.highpowerbear.hpboptions.entity;

import com.highpowerbear.hpboptions.model.Instrument;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Types;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Created by robertk on 11/20/2018.
 */
@Entity
@Table(name = "underlying", schema = "hpboptions", catalog = "hpboptions")
public class Underlying {
    private static final long serialVersionUID = 1162498913826827666L;

    @Id
    private Long id;
    private Integer conid;
    @Enumerated(EnumType.STRING)
    private Types.SecType secType;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Exchange exchange;
    @Enumerated(EnumType.STRING)
    private Exchange primaryExchange;
    private Integer optionMultiplier;
    private LocalTime marketOpen;
    private LocalTime marketClose;
    private Boolean active;

    public Instrument createInstrument() {
        return new Instrument(conid, secType, null, symbol, currency, exchange, primaryExchange);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Underlying that = (Underlying) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getConid() {
        return conid;
    }

    public void setConid(Integer conid) {
        this.conid = conid;
    }

    public Types.SecType getSecType() {
        return secType;
    }

    public void setSecType(Types.SecType secType) {
        this.secType = secType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
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

    public Integer getOptionMultiplier() {
        return optionMultiplier;
    }

    public void setOptionMultiplier(Integer optionMultiplier) {
        this.optionMultiplier = optionMultiplier;
    }

    public LocalTime getMarketOpen() {
        return marketOpen;
    }

    public void setMarketOpen(LocalTime marketOpen) {
        this.marketOpen = marketOpen;
    }

    public LocalTime getMarketClose() {
        return marketClose;
    }

    public void setMarketClose(LocalTime marketClose) {
        this.marketClose = marketClose;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
