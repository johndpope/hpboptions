package com.highpowerbear.hpboptions.dao;

import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.entity.Underlying;
import com.highpowerbear.hpboptions.entity.ImpliedVolatility;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by robertk on 11/8/2018.
 */
public interface CoreDao {
    List<IbOrder> getOpenIbOrders();
    void createIbOrder(IbOrder ibOrder);
    void updateIbOrder(IbOrder ibOrder);
    IbOrder getIbOrderByPermId(long permId);
    List<Underlying> getActiveUnderlyings();
    List<ImpliedVolatility> getImpliedVolatilities(LocalDate date);
    void createImpliedVolatility(ImpliedVolatility iv);
    void updateImpliedVolatility(ImpliedVolatility iv);
}
