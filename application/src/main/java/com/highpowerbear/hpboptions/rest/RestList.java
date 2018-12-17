package com.highpowerbear.hpboptions.rest;

import java.util.Collection;

/**
 * Created by robertk on 11/5/2018.
 */
public class RestList<T> {
    private final Collection<T> items;
    private final Long total;

    public RestList(Collection<T> items, Long total) {
        this.items = items;
        this.total = total;
    }

    public Long getTotal() {
        return total;
    }

    public Collection<T> getItems() {
        return items;
    }
}
