package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 2/4/2019.
 */
public class RiskThreshold {
    private Number low;
    private Number high;

    public RiskThreshold(Number low, Number high) {
        this.low = low;
        this.high = high;
    }

    public Number getLow() {
        return low;
    }

    public Number getHigh() {
        return high;
    }

    @Override
    public String toString() {
        return low + "," + high;
    }
}
