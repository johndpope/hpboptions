package com.highpowerbear.hpboptions.database;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by robertk on 11/8/2018.
 */
@Repository
@Transactional(propagation = Propagation.SUPPORTS)
public class HopDao {

    @PersistenceContext
    private EntityManager em;

    public List<Underlying> getActiveUnderlyings() {
        TypedQuery<Underlying> q = em.createQuery("SELECT u FROM Underlying u WHERE u.active = TRUE ORDER BY u.displayRank", Underlying.class);
        return q.getResultList();
    }
}