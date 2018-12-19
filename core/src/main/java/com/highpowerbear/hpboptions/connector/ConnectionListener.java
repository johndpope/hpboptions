package com.highpowerbear.hpboptions.connector;

/**
 * Created by robertk on 12/19/2018.
 */
public interface ConnectionListener {
    default void preConnect() {}
    default void postConnect() {}
    default void preDisconnect() {}
    default void postDisconnect() {}
}
