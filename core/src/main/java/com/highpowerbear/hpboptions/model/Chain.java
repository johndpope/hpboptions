package com.highpowerbear.hpboptions.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robertk on 12/24/2018.
 */
public class Chain {

    private int underlyingConid;
    private String underlyingSymbol;
    private LocalDate expiration;
    private final SortedMap<Double, ChainItem> itemMap = new TreeMap<>(); // strike -> (call, put)

    private AtomicBoolean loaded = new AtomicBoolean(false);

    public Chain(int underlyingConid, String underlyingSymbol, LocalDate expiration) {
        this.underlyingConid = underlyingConid;
        this.underlyingSymbol = underlyingSymbol;
        this.expiration = expiration;
    }

    public void addItem(ChainItem item) {
        itemMap.put(item.getStrike(), item);
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

    public Collection<ChainItem> getItems() {
        return itemMap.values();
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void setLoaded(boolean loaded) {
        this.loaded.set(true);
    }
}
