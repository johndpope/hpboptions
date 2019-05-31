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

    public OptionInstrument(int conid, Types.SecType secType, String underlyingSymbol, String symbol, Currency currency, Types.Right right, double strike, LocalDate expiration) {
        super(conid, secType, underlyingSymbol, symbol, currency, expiration);
        this.right = right;
        this.strike = strike;
    }

    public boolean isCall() {
        return right == Types.Right.Call;
    }

    public boolean isPut() {
        return right == Types.Right.Put;
    }

    public Types.Right getRight() {
        return right;
    }

    public double getStrike() {
        return strike;
    }
}
