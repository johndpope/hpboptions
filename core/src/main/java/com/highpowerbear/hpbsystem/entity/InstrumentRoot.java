package com.highpowerbear.hpbsystem.entity;

import com.highpowerbear.hpbsystem.enums.Currency;
import com.highpowerbear.hpbsystem.enums.Exchange;
import com.highpowerbear.hpbsystem.enums.SecType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by robertk on 10/28/2018.
 */
@Entity
@Table(name = "instrument", schema = "hpbsystem", catalog = "hpbsystem")
public class InstrumentRoot implements Serializable {
    private static final long serialVersionUID = 7243224755428283653L;

    @Id
    private Long id;
    private String underlying;
    @Enumerated(EnumType.STRING)
    private SecType secType;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Exchange exchange;
    private Integer multiplier;
    private Double priceIncrement;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrumentRoot that = (InstrumentRoot) o;

        return id != null ? id.equals(that.id) : that.id == null;
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

    public String getUnderlying() {
        return underlying;
    }

    public void setUnderlying(String underlying) {
        this.underlying = underlying;
    }

    public SecType getSecType() {
        return secType;
    }

    public void setSecType(SecType secType) {
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

    public Double getPriceIncrement() {
        return priceIncrement;
    }

    public void setPriceIncrement(Double priceIncrement) {
        this.priceIncrement = priceIncrement;
    }
}
