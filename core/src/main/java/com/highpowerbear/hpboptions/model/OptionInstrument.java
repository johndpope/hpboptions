package com.highpowerbear.hpboptions.model;

import com.highpowerbear.hpboptions.enums.Currency;
import com.ib.client.Types;

import java.time.LocalDate;

/**
 * Created by robertk on 12/18/2018.
 */
public class OptionInstrument extends Instrument {

    private final Types.Right right;
    private final double strike;
    private final LocalDate expirationDate;
    private final int multiplier;
    private double minTick;
    private Types.SecType underlyingSecType;
    private int underlyingConid;
    private final String underlyingSymbol;

    public OptionInstrument(int conid, Types.SecType secType, String symbol, Currency currency, Types.Right right, double strike, LocalDate expirationDate, int multiplier, String underlyingSymbol) {
        super(conid, secType, symbol, currency);
        this.right = right;
        this.strike = strike;
        this.expirationDate = expirationDate;
        this.multiplier = multiplier;
        this.underlyingSymbol = underlyingSymbol;
    }

    public Types.Right getRight() {
        return right;
    }

    public double getStrike() {
        return strike;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public double getMinTick() {
        return minTick;
    }

    public void setMinTick(double minTick) {
        this.minTick = minTick;
    }

    public void setUnderlyingConid(int underlyingConid) {
        this.underlyingConid = underlyingConid;
    }

    public Types.SecType getUnderlyingSecType() {
        return underlyingSecType;
    }

    public void setUnderlyingSecType(Types.SecType underlyingSecType) {
        this.underlyingSecType = underlyingSecType;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public int getUnderlyingConid() {
        return underlyingConid;
    }
}
