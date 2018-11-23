package com.highpowerbear.hpboptions.common;

import com.ib.client.Contract;

/**
 * Created by robertk on 10/28/2018.
 */
public class CoreUtil {

    public static void waitMilliseconds(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    public static String contractDetails(Contract c) {
        return c.localSymbol() + ", " + c.symbol() + ", " + c.secType() + ", " + c.lastTradeDateOrContractMonth() +
                ", " + c.right() + ", " + c.exchange() + ", " + c.currency() + ", " + c.multiplier() + ", " +  c.includeExpired();
    }
}