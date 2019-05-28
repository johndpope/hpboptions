package com.highpowerbear.hpboptions.database;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.ib.client.Types;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Created by robertk on 5/24/2019.
 */
@Entity
@Table(name = "linear_instrument", schema = "hpboptions", catalog = "hpboptions")
public class LinearInstrument implements Serializable {
    private static final long serialVersionUID = -1847062490283602339L;

    @Id
    private Long id;
    private Integer conid;
    @Enumerated(EnumType.STRING)
    private Types.SecType secType;
    private String symbolRoot;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Exchange exchange;
    private Double multiplier;
    private Double minTick;
    private LocalDate expirationDate;
    private boolean active;
    private Integer displayRank;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinearInstrument that = (LinearInstrument) o;

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

    public String getSymbolRoot() {
        return symbolRoot;
    }

    public void setSymbolRoot(String symbolRoot) {
        this.symbolRoot = symbolRoot;
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

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }

    public Double getMinTick() {
        return minTick;
    }

    public void setMinTick(Double minTick) {
        this.minTick = minTick;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getDisplayRank() {
        return displayRank;
    }

    public void setDisplayRank(Integer displayRank) {
        this.displayRank = displayRank;
    }
}
