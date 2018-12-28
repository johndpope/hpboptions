package com.highpowerbear.hpboptions.model;

import java.time.LocalDate;

/**
 * Created by robertk on 12/28/2018.
 */
public class ChainInfo {
    private final int underlyingConid;
    private final String underlyingSymbol;
    private final LocalDate expiration;

    public ChainInfo(int underlyingConid, String underlyingSymbol, LocalDate expiration) {
        this.underlyingConid = underlyingConid;
        this.underlyingSymbol = underlyingSymbol;
        this.expiration = expiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChainInfo chainInfo = (ChainInfo) o;

        if (underlyingConid != chainInfo.underlyingConid) return false;
        return expiration.equals(chainInfo.expiration);
    }

    @Override
    public int hashCode() {
        int result = underlyingConid;
        result = 31 * result + expiration.hashCode();
        return result;
    }

    public int getUnderlyingConid() {
        return underlyingConid;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public LocalDate getExpiration() {
        return expiration;
    }
}
