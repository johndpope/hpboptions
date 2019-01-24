package com.highpowerbear.hpboptions.database;

import com.highpowerbear.hpboptions.enums.UnderlyingDataField;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Created by robertk on 1/24/2019.
 */
@Entity
@Table(name = "underlying_alert", schema = "hpboptions", catalog = "hpboptions")
public class UnderlyingAlert implements Serializable {
    private static final long serialVersionUID = -4448199060498759078L;

    @Id
    @SequenceGenerator(name="underlying_alert_generator", sequenceName = "underlying_alert_seq", schema = "hpboptions", catalog = "hpboptions", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "underlying_alert_generator")
    private Long id;
    private LocalDateTime date;
    private String symbol;
    @Enumerated(EnumType.STRING)
    private UnderlyingDataField dataField;
    private String condition;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnderlyingAlert alert = (UnderlyingAlert) o;

        return Objects.equals(id, alert.id);
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public UnderlyingDataField getDataField() {
        return dataField;
    }

    public void setDataField(UnderlyingDataField dataField) {
        this.dataField = dataField;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
