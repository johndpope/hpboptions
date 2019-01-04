package com.highpowerbear.hpboptions.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.highpowerbear.hpboptions.common.CoreSettings;
import com.highpowerbear.hpboptions.enums.Currency;
import com.highpowerbear.hpboptions.enums.Exchange;
import com.highpowerbear.hpboptions.enums.OrderStatus;
import com.highpowerbear.hpboptions.enums.SubmitType;
import com.ib.client.OrderType;
import com.ib.client.Types;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by robertk on 10/27/2018.
 */
@Entity
@Table(name = "ib_order", schema = "hpboptions", catalog = "hpboptions")
public class IbOrder implements Serializable {
    private static final long serialVersionUID = -1176562276081157205L;

    @Id
    @SequenceGenerator(name="ib_order_generator", sequenceName = "ib_order_seq", schema = "hpboptions", catalog = "hpboptions", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ib_order_generator")
    private Long id;
    private Long permId;
    private Integer orderId;
    @Enumerated(EnumType.STRING)
    private Types.Action action;
    private Integer quantity;
    @Enumerated(EnumType.STRING)
    private Types.SecType secType;
    private String underlyingSymbol;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    private Exchange exchange;
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    @JsonFormat(pattern = CoreSettings.JSON_DATETIME_FORMAT)
    private LocalDateTime submitDate;
    private Double orderPrice;
    @JsonFormat(pattern = CoreSettings.JSON_DATETIME_FORMAT)
    private LocalDateTime statusDate;
    private Double fillPrice;
    @Enumerated(EnumType.STRING)
    private SubmitType submitType;
    private String triggerDesc;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @OneToMany(mappedBy = "ibOrder", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @OrderBy("eventDate DESC, id DESC")
    private List<IbOrderEvent> ibOrderEvents;
    @Transient
    private Integer heartbeatCount;

    public void addEvent(OrderStatus status, Double price) {
        this.status = status;
        this.statusDate = LocalDateTime.now();
        IbOrderEvent e = new IbOrderEvent();
        e.setIbOrder(this);
        e.setEventDate(this.statusDate);
        e.setStatus(this.status);
        e.setPrice(price);
        if (OrderStatus.SUBMITTED.equals(status)) {
            this.submitDate = this.statusDate;
        }
        if (OrderStatus.FILLED.equals(status)) {
            this.fillPrice = price;
        }
        if (ibOrderEvents == null) {
            ibOrderEvents = new ArrayList<>();
        }
        ibOrderEvents.add(e);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IbOrder ibOrder = (IbOrder) o;

        return Objects.equals(id, ibOrder.id);
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

    public Long getPermId() {
        return permId;
    }

    public void setPermId(Long permId) {
        this.permId = permId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Types.Action getAction() {
        return action;
    }

    public void setAction(Types.Action action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Types.SecType getSecType() {
        return secType;
    }

    public void setSecType(Types.SecType secType) {
        this.secType = secType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public LocalDateTime getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(LocalDateTime submitDate) {
        this.submitDate = submitDate;
    }

    public Double getOrderPrice() {
        return orderPrice;
    }

    public void setOrderPrice(Double orderPrice) {
        this.orderPrice = orderPrice;
    }

    public LocalDateTime getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(LocalDateTime fillDate) {
        this.statusDate = fillDate;
    }

    public Double getFillPrice() {
        return fillPrice;
    }

    public void setFillPrice(Double fillPrice) {
        this.fillPrice = fillPrice;
    }

    public SubmitType getSubmitType() {
        return submitType;
    }

    public void setSubmitType(SubmitType submitType) {
        this.submitType = submitType;
    }

    public String getTriggerDesc() {
        return triggerDesc;
    }

    public void setTriggerDesc(String triggerDesc) {
        this.triggerDesc = triggerDesc;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<IbOrderEvent> getIbOrderEvents() {
        return ibOrderEvents;
    }

    public void setIbOrderEvents(List<IbOrderEvent> events) {
        this.ibOrderEvents = events;
    }

    public Integer getHeartbeatCount() {
        return heartbeatCount;
    }

    public void setHeartbeatCount(Integer heartbeatCount) {
        this.heartbeatCount = heartbeatCount;
    }
}
