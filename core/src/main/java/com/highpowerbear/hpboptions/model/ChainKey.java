package com.highpowerbear.hpboptions.model;

import java.time.LocalDate;

/**
 * Created by robertk on 1/5/2019.
 */
public class ChainKey {

    private final int underlyingConid;
    private final String underlyingSymbol;
    private final LocalDate expiration;

    public ChainKey(int underlyingConid, String underlyingSymbol, LocalDate expiration) {
        this.underlyingConid = underlyingConid;
        this.underlyingSymbol = underlyingSymbol;
        this.expiration = expiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChainKey chainKey = (ChainKey) o;

        if (underlyingConid != chainKey.underlyingConid) return false;
        return expiration.equals(chainKey.expiration);
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

    @Override
    public String toString() {
        return underlyingConid + "_" + expiration;
    }
}
