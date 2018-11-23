package com.highpowerbear.hpboptions.dao;

import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.entity.OptionRoot;

import java.util.List;

/**
 * Created by robertk on 11/8/2018.
 */
public interface CoreDao {
    List<IbOrder> getOpenIbOrders();
    void createIbOrder(IbOrder ibOrder);
    void updateIbOrder(IbOrder ibOrder);
    IbOrder getIbOrderByPermId(long permId);
    List<OptionRoot> getActiveOptionRoots();
}
