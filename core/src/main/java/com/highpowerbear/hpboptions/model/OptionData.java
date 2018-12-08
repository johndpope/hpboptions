package com.highpowerbear.hpboptions.model;

/**
 * Created by robertk on 12/6/2018.
 */
public class OptionData {

    private double delta = Double.NaN;
    private double gamma = Double.NaN;
    private double vega = Double.NaN;
    private double theta = Double.NaN;
    private double impliedVolatility = Double.NaN;
    private double optionPrice = Double.NaN;
    private double underlyingPrice = Double.NaN;

    // calculated
    private double timeValue = Double.NaN;
    private double timeValuePct = Double.NaN;

    void update(double delta, double gamma, double vega, double theta, double impliedVolatility, double underlyingPrice, double optionPrice, double timeValue, double timeValuePct) {
        this.delta = delta;
        this.gamma = gamma;
        this.vega = vega;
        this.theta = theta;
        this.impliedVolatility = impliedVolatility;
        this.underlyingPrice = underlyingPrice;
        this.optionPrice = optionPrice;
        this.timeValue = timeValue;
        this.timeValuePct = timeValuePct;
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

    public double getImpliedVolatility() {
        return impliedVolatility;
    }

    public double getOptionPrice() {
        return optionPrice;
    }

    public double getUnderlyingPrice() {
        return underlyingPrice;
    }

    public double getTimeValue() {
        return timeValue;
    }

    public double getTimeValuePct() {
        return timeValuePct;
    }

    public String toCsvString() {
        return "optionData," + delta + "," + gamma + "," + vega + "," + theta + "," + impliedVolatility + "," + optionPrice + "," + underlyingPrice;
    }
}
