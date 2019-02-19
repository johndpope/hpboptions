package com.highpowerbear.hpboptions.database;

import com.highpowerbear.hpboptions.common.HopSettings;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.enums.RiskResolution;
import com.ib.client.Contract;
import com.ib.client.Types;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Created by robertk on 11/20/2018.
 */
@Entity
@Table(name = "underlying", schema = "hpboptions", catalog = "hpboptions")
public class Underlying implements Serializable {
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
    private LocalTime marketOpen;
    private LocalTime marketClose;
    private boolean active;
    private Integer displayRank;
    private Integer chainMultiplier;
    private boolean chainRoundStrikes;
    @Enumerated(EnumType.STRING)
    private Exchange chainExchange;
    private Integer cfdConid;
    private String cfdSymbol;
    private Double cfdMinTick;
    @Enumerated(EnumType.STRING)
    private RiskResolution riskResolution;

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

    public Contract createChainRequestContract(LocalDate expiration) {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType(Types.SecType.OPT);
        contract.lastTradeDateOrContractMonth(expiration.format(HopSettings.IB_DATE_FORMATTER));
        contract.currency(currency.name());
        contract.exchange(chainExchange.name());

        return contract;
    }

    public boolean isCfdDefined() {
        return cfdConid != null && cfdSymbol != null && cfdMinTick != null;
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

    public Integer getChainMultiplier() {
        return chainMultiplier;
    }

    public void setChainMultiplier(Integer chainMultiplier) {
        this.chainMultiplier = chainMultiplier;
    }

    public boolean isChainRoundStrikes() {
        return chainRoundStrikes;
    }

    public void setChainRoundStrikes(boolean chainRoundStrikes) {
        this.chainRoundStrikes = chainRoundStrikes;
    }

    public Exchange getChainExchange() {
        return chainExchange;
    }

    public void setChainExchange(Exchange chainExchange) {
        this.chainExchange = chainExchange;
    }

    public Integer getCfdConid() {
        return cfdConid;
    }

    public void setCfdConid(Integer cfdConid) {
        this.cfdConid = cfdConid;
    }

    public String getCfdSymbol() {
        return cfdSymbol;
    }

    public void setCfdSymbol(String cfdSymbol) {
        this.cfdSymbol = cfdSymbol;
    }

    public Double getCfdMinTick() {
        return cfdMinTick;
    }

    public void setCfdMinTick(Double cfdMinTick) {
        this.cfdMinTick = cfdMinTick;
    }

    public RiskResolution getRiskResolution() {
        return riskResolution;
    }

    public void setRiskResolution(RiskResolution riskResolution) {
        this.riskResolution = riskResolution;
    }
}
