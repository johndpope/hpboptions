package com.highpowerbear.hpboptions.model;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by robertk on 12/20/2018.
 */
public class ChainParams {

    private int underlyingConid;
    private String underlyingSymbol;
    private SortedSet<String> expiries = new TreeSet<>();

    public void setUnderlying(int underlyingConid, String underlyingSymbol) {
        this.underlyingConid = underlyingConid;
        this.underlyingSymbol = underlyingSymbol;
        expiries.clear();
    }

    public void setExpiries(Set<String> expiries) {
        this.expiries.clear();
        this.expiries.addAll(expiries);
    }

    public int getUnderlyingConid() {
        return underlyingConid;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public SortedSet<String> getExpiries() {
        return expiries;
    }
}
