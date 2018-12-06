package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 12/6/2018.
 */
public class CumulativeOptionData {

    private double delta = Double.NaN;
    private double gamma = Double.NaN;
    private double vega = Double.NaN;
    private double theta = Double.NaN;
    private double deltaDollars = Double.NaN;
    private double timeValue = Double.NaN;

    void update(double delta, double gamma, double vega, double theta, double deltaDollars, double timeValue) {
        this.delta = delta;
        this.gamma = gamma;
        this.vega = vega;
        this.theta = theta;
        this.deltaDollars = deltaDollars;
        this.timeValue = timeValue;
    }

    public double getDelta() {
        return delta;
    }

    public double getGamma() {
        return gamma;
    }

    public double getVega() {
        return vega;
    }

    public double getTheta() {
        return theta;
    }

    public double getDeltaDollars() {
        return deltaDollars;
    }

    public double getTimeValue() {
        return timeValue;
    }
}
