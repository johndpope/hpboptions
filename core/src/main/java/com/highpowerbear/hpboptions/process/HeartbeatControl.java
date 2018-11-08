package com.highpowerbear.hpboptions.process;

import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.dao.ProcessDao;
import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by robertk on 4/6/2015.
 */
@Component
public class HeartbeatControl {

    private final ProcessDao processDao;
    private final Map<IbOrder, Integer> openOrderHeartbeatMap = new ConcurrentHashMap<>(); // ibOrder --> number of failed heartbeats left before UNKNOWN

    @Autowired
    public HeartbeatControl(ProcessDao processDao) {
        this.processDao = processDao;
    }

    @PostConstruct
    public void init() {
        processDao.getOpenIbOrders().forEach(this::initHeartbeat);
    }

    public Map<IbOrder, Integer> getOpenOrderHeartbeatMap() {
        return openOrderHeartbeatMap;
    }

    public void updateHeartbeats(String accountId) {
        Set<IbOrder> ibOrders = new HashSet<>(openOrderHeartbeatMap.keySet());

        for (IbOrder ibOrder : ibOrders) {
            Integer failedHeartbeatsLeft = openOrderHeartbeatMap.get(ibOrder);

            if (failedHeartbeatsLeft <= 0) {
                if (!OrderStatus.UNKNOWN.equals(ibOrder.getStatus())) {
                    ibOrder.addEvent(OrderStatus.UNKNOWN, null);
                    processDao.updateIbOrder(ibOrder);
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