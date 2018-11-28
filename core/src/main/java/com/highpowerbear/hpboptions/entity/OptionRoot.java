package com.highpowerbear.hpboptions.entity;

import com.highpowerbear.hpboptions.corelogic.model.Instrument;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Types;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Created by robertk on 11/20/2018.
 */
@Entity
@Table(name = "option_root", schema = "hpboptions", catalog = "hpboptions")
public class OptionRoot {
    private static final long serialVersionUID = 1162498913826827666L;

    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    private Types.SecType secType;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Exchange exchange;
    private Integer multiplier;
    @Enumerated(EnumType.STRING)
    private Types.SecType undlSecType;
    private String undlSymbol;
    @Enumerated(EnumType.STRING)
    private Exchange undlExchange;
    @Enumerated(EnumType.STRING)
    private Exchange undlPrimaryExchange;
    private Boolean active;

    public Instrument getUnderlyingInstrument() {
        return new Instrument(undlSecType, undlSymbol, undlSymbol, currency, undlExchange, undlPrimaryExchange);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OptionRoot that = (OptionRoot) o;

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

    public Types.SecType getSecType() {
        return secType;
    }

    public void setSecType(Types.SecType secType) {
        this.secType = secType;
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

    public Integer getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Integer multiplier) {
        this.multiplier = multiplier;
    }

    public Types.SecType getUndlSecType() {
        return undlSecType;
    }

    public void setUndlSecType(Types.SecType undlSecType) {
        this.undlSecType = undlSecType;
    }

    public String getUndlSymbol() {
        return undlSymbol;
    }

    public void setUndlSymbol(String undlSymbol) {
        this.undlSymbol = undlSymbol;
    }

    public Exchange getUndlExchange() {
        return undlExchange;
    }

    public void setUndlExchange(Exchange undlExchange) {
        this.undlExchange = undlExchange;
    }

    public Exchange getUndlPrimaryExchange() {
        return undlPrimaryExchange;
    }

    public void setUndlPrimaryExchange(Exchange undlPrimaryExchange) {
        this.undlPrimaryExchange = undlPrimaryExchange;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
