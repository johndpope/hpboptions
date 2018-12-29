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

    public static double round(double number, int decimalPlaces) {
        double modifier = Math.pow(10.0, decimalPlaces);
        return Math.round(number * modifier) / modifier;
    }

    public static double round4(double number) {
        return Double.isNaN(number) ? number : round(number, 4);
    }

    public static  boolean isValidPrice(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && d > 0d && d != Double.MAX_VALUE;
    }

    public static  boolean isValidSize(int i) {
        return i > 0 && i != Integer.MAX_VALUE;
    }

    public static boolean isRound(double value) {
        return value == Math.round(value);
    }
}