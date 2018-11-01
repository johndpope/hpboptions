package com.highpowerbear.hpboptions.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.enums.OrderStatus;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Created by robertk on 10/27/2018.
 */
@Entity
@Table(name = "ib_order_event", schema = "hpboptions", catalog = "hpboptions")
public class IbOrderEvent implements Serializable {
    private static final long serialVersionUID = 7594577069454549547L;

    @Id
    @SequenceGenerator(name="ib_order_event_generator", sequenceName = "ib_order_event_seq", schema = "hpboptions", catalog = "hpboptions", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ib_order_event_generator")
    private Long id;
    @JsonFormat(pattern = CoreSettings.JSON_DATE_FORMAT)
    private LocalDateTime eventDate;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private Double price;
    @JsonIgnore
    @ManyToOne
    private IbOrder ibOrder;

    @JsonProperty
    public Long getIbOrderDbId() {
        return ibOrder.getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IbOrderEvent that = (IbOrderEvent) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public IbOrder getIbOrder() {
        return ibOrder;
    }

    public void setIbOrder(IbOrder ibOrder) {
        this.ibOrder = ibOrder;
    }
}
