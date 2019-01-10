package com.highpowerbear.hpboptions.rest.model;

import java.util.Collection;

/**
 * Created by robertk on 11/5/2018.
 */
public class RestList<T> {
    private final Collection<T> items;
    private final int total;

    public RestList(Collection<T> items, int total) {
        this.items = items;
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public Collection<T> getItems() {
        return items;
    }
}
