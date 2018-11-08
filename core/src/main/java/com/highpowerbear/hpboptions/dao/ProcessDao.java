package com.highpowerbear.hpboptions.dao;

import com.highpowerbear.hpboptions.entity.IbOrder;

import java.util.List;

/**
 * Created by robertk on 11/8/2018.
 */
public interface ProcessDao {
    List<IbOrder> getOpenIbOrders();
    void newIbOrder(IbOrder ibOrder);
    void updateIbOrder(IbOrder ibOrder);
    IbOrder getIbOrderByPermId(long permId);
}
