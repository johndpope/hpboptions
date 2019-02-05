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
    private final LocalDate expiration;
    private final int multiplier;

    public OptionInstrument(int conid, Types.SecType secType, String underlyingSymbol, String symbol, Currency currency, Types.Right right, double strike, LocalDate expiration, int multiplier) {
        super(conid, secType, underlyingSymbol, symbol, currency);
        this.right = right;
        this.strike = strike;
        this.expiration = expiration;
        this.multiplier = multiplier;
    }

    public Types.Right getRight() {
        return right;
    }

    public double getStrike() {
        return strike;
    }

    public LocalDate getExpiration() {
        return expiration;
    }

    public int getMultiplier() {
        return multiplier;
    }
}
