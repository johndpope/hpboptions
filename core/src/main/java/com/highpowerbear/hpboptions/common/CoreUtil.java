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
            // Ignore
        }
    }

    public static String printIbContract(Contract contract) {
        return  contract.localSymbol() + ", " + contract.symbol() + ", " + contract.secType() + ", " + contract.lastTradeDateOrContractMonth() + ", " + contract.right() + ", " +
                contract.exchange() + ", " + contract.currency() + ", " + contract.multiplier() + ", " +  contract.includeExpired();
    }

    public static String getContractInfo(Contract contract) {
        return contract.localSymbol() + "-" + contract.currency() + "-" + contract.exchange();
    }

    public static Contract createContract(String contractInfo) {
        // TODO
        return new Contract();
    }
}
