package com.highpowerbear.hpboptions.dao;

import com.highpowerbear.hpboptions.entity.IbOrder;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by robertk on 11/8/2018.
 */
@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public class CoreDaoImpl implements CoreDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<IbOrder> getOpenIbOrders() {
        TypedQuery<IbOrder> q = em.createQuery("SELECT io FROM IbOrder io WHERE io.status IN :statuses", IbOrder.class);

        Set<OrderStatus> statuses = new HashSet<>();
        statuses.add(OrderStatus.SUBMITTED);
        statuses.add(OrderStatus.UPDATED);
        q.setParameter("statuses", statuses);

        return q.getResultList();
    }

    @Transactional
    @Override
    public void createIbOrder(IbOrder ibOrder) {
        em.persist(ibOrder);
    }

    @Transactional
    @Override
    public void updateIbOrder(IbOrder ibOrder) {
        em.merge(ibOrder);
    }

    @Override
    public IbOrder getIbOrderByPermId(long permId) {
        TypedQuery<IbOrder> q = em.createQuery("SELECT io FROM IbOrder io WHERE io.permId = :permId", IbOrder.class);

        q.setParameter("permId", permId);
        List<IbOrder> ibOrders = q.getResultList();

        return !ibOrders.isEmpty() ? ibOrders.get(0) : null;
    }

    @Override
    public List<OptionRoot> getActiveOptionRoots() {
        TypedQuery<OptionRoot> q = em.createQuery("SELECT or FROM OptionRoot or WHERE or.active = TRUE ORDER BY or.id", OptionRoot.class);
        return q.getResultList();
    }
}