package com.highpowerbear.hpboptions.entity;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.enums.SecType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by robertk on 10/28/2018.
 */
@Entity
@Table(name = "contract_root", schema = "hpboptions", catalog = "hpboptions")
public class ContractRoot implements Serializable {
    private static final long serialVersionUID = 7243224755428283653L;

    @Id
    private Long id;
    @Enumerated(EnumType.STRING)
    private SecType secType;
    private Currency currency;
    private Exchange exchange;
    private Integer multiplier;
    @ManyToOne
    private Underlying underlying;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContractRoot that = (ContractRoot) o;

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

    public Underlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(Underlying underlying) {
        this.underlying = underlying;
    }
}