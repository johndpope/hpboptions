package com.highpowerbear.hpboptions.logic;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by robertk on 4/6/2015.
 */
@Component
public class HeartbeatMonitor {

    private final CoreDao coreDao;
    private final Map<IbOrder, Integer> openOrderHeartbeatMap = new HashMap<>(); // ibOrder -> number of failed heartbeats left before UNKNOWN

    @Autowired
    public HeartbeatMonitor(CoreDao coreDao) {
        this.coreDao = coreDao;
    }

    @PostConstruct
    public void init() {
        coreDao.getOpenIbOrders().forEach(this::initHeartbeat);
    }

    public Map<IbOrder, Integer> getOpenOrderHeartbeatMap() {
        return openOrderHeartbeatMap;
    }

    public void updateHeartbeats() {
        Set<IbOrder> ibOrders = new HashSet<>(openOrderHeartbeatMap.keySet());

        for (IbOrder ibOrder : ibOrders) {
            Integer failedHeartbeatsLeft = openOrderHeartbeatMap.get(ibOrder);

            if (failedHeartbeatsLeft <= 0) {
                if (!OrderStatus.UNKNOWN.equals(ibOrder.getStatus())) {
                    ibOrder.addEvent(OrderStatus.UNKNOWN, null);
                    coreDao.updateIbOrder(ibOrder);
                }
                openOrderHeartbeatMap.remove(ibOrder);
            } else {
                openOrderHeartbeatMap.put(ibOrder, failedHeartbeatsLeft - 1);
            }
        }
    }

    public void initHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.put(ibOrder, CoreSettings.MAX_ORDER_HEARTBEAT_FAILS);
    }

    public void removeHeartbeat(IbOrder ibOrder) {
        openOrderHeartbeatMap.remove(ibOrder);
    }
}