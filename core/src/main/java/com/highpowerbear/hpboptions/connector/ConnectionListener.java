package com.highpowerbear.hpboptions.connector;

/**
 * Created by robertk on 12/19/2018.
 */
public interface ConnectionListener {
    void preConnect();
    void postConnect();
    void preDisconnect();
    void postDisconnect();
}
