package com.highpowerbear.hpboptions.database;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Created by robertk on 1/24/2019.
 */
@Entity
@Table(name = "risk_event", schema = "hpboptions", catalog = "hpboptions")
public class RiskEvent implements Serializable {
    private static final long serialVersionUID = -4448199060498759078L;

    @Id
    @SequenceGenerator(name="risk_event_generator", sequenceName = "risk_event_seq", schema = "hpboptions", catalog = "hpboptions", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "risk_event_generator")
    private Long id;
    private LocalDateTime date;
    private int underlyingConid;
    private String underlyingSymbol;
    private String dataField;
    private String fieldValue;
    private String fieldThreshold;
    private String resolution;
    private String ibAccount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RiskEvent riskEvent = (RiskEvent) o;

        return Objects.equals(id, riskEvent.id);
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

    public int getUnderlyingConid() {
        return underlyingConid;
    }

    public void setUnderlyingConid(int underlyingConid) {
        this.underlyingConid = underlyingConid;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public void setUnderlyingSymbol(String underlyingSymbol) {
        this.underlyingSymbol = underlyingSymbol;
    }

    public String getDataField() {
        return dataField;
    }

    public void setDataField(String dataField) {
        this.dataField = dataField;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getFieldThreshold() {
        return fieldThreshold;
    }

    public void setFieldThreshold(String fieldThreshold) {
        this.fieldThreshold = fieldThreshold;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getIbAccount() {
        return ibAccount;
    }

    public void setIbAccount(String ibAccount) {
        this.ibAccount = ibAccount;
    }
}
