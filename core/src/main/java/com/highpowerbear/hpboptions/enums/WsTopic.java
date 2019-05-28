package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 12/19/2018.
 */
public enum WsTopic {
    IB_CONNECTION,
    ACCOUNT,
    UNDERLYING,
    ORDER,
    POSITION,
    CHAIN,
    SCANNER,
    LINEAR;

    public String suffix() {
        return this.name().toLowerCase();
    }
}
