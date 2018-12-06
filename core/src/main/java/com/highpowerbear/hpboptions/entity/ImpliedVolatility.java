package com.highpowerbear.hpboptions.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Created by robertk on 12/5/2018.
 */
@Entity
@Table(name = "implied_volatility", schema = "hpboptions", catalog = "hpboptions")
public class ImpliedVolatility implements Serializable {
    private static final long serialVersionUID = -4116529982506081333L;

    @Id
    @SequenceGenerator(name="iv_generator", sequenceName = "iv_seq", schema = "hpboptions", catalog = "hpboptions", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "iv_generator")
    private Long id;
    private LocalDate date;
    private String underlyingDescriptor;
    private Double value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImpliedVolatility that = (ImpliedVolatility) o;

        return Objects.equals(id, that.id);
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getUnderlyingDescriptor() {
        return underlyingDescriptor;
    }

    public void setUnderlyingDescriptor(String underlyingDescriptor) {
        this.underlyingDescriptor = underlyingDescriptor;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
