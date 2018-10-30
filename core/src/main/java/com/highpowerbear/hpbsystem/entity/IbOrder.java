package com.highpowerbear.hpbsystem.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.highpowerbear.hpbsystem.common.CoreSettings;
import com.highpowerbear.hpbsystem.enums.Action;
import com.highpowerbear.hpbsystem.enums.Currency;
import com.highpowerbear.hpbsystem.enums.OrderStatus;
import com.highpowerbear.hpbsystem.enums.OrderType;
import com.highpowerbear.hpbsystem.enums.SecType;
import com.highpowerbear.hpbsystem.enums.SubmitType;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by robertk on 10/27/2018.
 */
@Entity
@Table(name = "ib_order", schema = "hpbsystem", catalog = "hpbsystem")
public class IbOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name="ib_order_generator", sequenceName = "ib_order_seq", schema = "hpbanalytics", catalog = "hpbanalytics", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ib_order_generator")
    private Long id;
    private Long permId;
    private Integer orderId;
    private String ibAccountId;
    @Enumerated(EnumType.STRING)
    private Action action;
    private Integer quantity;
    @ManyToOne
    private Underlying underlying;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private SecType secType;
    @Enumerated(EnumType.STRING)
    private OrderType orderType;
    @JsonFormat(pattern = CoreSettings.JSON_DATE_FORMAT)
    private LocalDateTime submitDate;
    private Double orderPrice;
    @JsonFormat(pattern = CoreSettings.JSON_DATE_FORMAT)
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

        return !(id != null ? !id.equals(ibOrder.id) : ibOrder.id != null);

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

    public String getIbAccountId() {
        return ibAccountId;
    }

    public void setIbAccountId(String ibAccountId) {
        this.ibAccountId = ibAccountId;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Underlying getUnderlying() {
        return underlying;
    }

    public void setUnderlying(Underlying underlying) {
        this.underlying = underlying;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public SecType getSecType() {
        return secType;
    }

    public void setSecType(SecType secType) {
        this.secType = secType;
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
