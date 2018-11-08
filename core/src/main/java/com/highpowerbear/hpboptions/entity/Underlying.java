package com.highpowerbear.hpboptions.entity;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.enums.SecType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by robertk on 11/8/2018.
 */
@Entity
@Table(name = "underlying", schema = "hpboptions", catalog = "hpboptions")
public class Underlying implements Serializable {
    private static final long serialVersionUID = -6514046905649346983L;

    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    private SecType secType;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private Exchange exchange;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Underlying that = (Underlying) o;

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

    public SecType getSecType() {
        return secType;
    }

    public void setSecType(SecType secType) {
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
}
