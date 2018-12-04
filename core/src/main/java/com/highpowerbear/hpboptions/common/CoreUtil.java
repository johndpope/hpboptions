package com.highpowerbear.hpboptions.common;

import com.ib.client.Contract;
import org.apache.commons.text.CaseUtils;

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

    public static String toCamelCase(String name) {
        return CaseUtils.toCamelCase(name, false, '_');
    }
}