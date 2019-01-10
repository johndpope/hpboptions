package com.highpowerbear.hpboptions.rest.model;

import com.ib.client.Types;

/**
 * Created by robertk on 1/10/2019.
 */
public class CreateOrderParams {
    private int conid;
    private Types.Action action;

    public int getConid() {
        return conid;
    }

    public Types.Action getAction() {
        return action;
    }
}
