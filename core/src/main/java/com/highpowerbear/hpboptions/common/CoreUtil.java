package com.highpowerbear.hpboptions.common;

import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.process.model.Instrument;
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

    public static Instrument instrument(Contract c) {
        return new Instrument(c.secType(), c.localSymbol(), c.symbol(), Currency.valueOf(c.currency()), Exchange.valueOf(c.exchange()));
    }

    public static Contract contract(Instrument instrument) {
        Contract contract = new Contract();

        contract.localSymbol(instrument.getSymbol());
        contract.symbol(instrument.getUnderlying());
        contract.secType(instrument.getSecType());
        contract.exchange(instrument.getExchange().name());
        contract.currency(instrument.getCurrency().name());

        return contract;
    }
}
