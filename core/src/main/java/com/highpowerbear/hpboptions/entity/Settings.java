package com.highpowerbear.hpboptions.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Created by robertk on 10/30/2018.
 */
@Entity
@Table(name = "instrument_settings", schema = "hpboptions", catalog = "hpboptions")
public class Settings implements Serializable {
    private static final long serialVersionUID = -3698129876605166001L;

    @Id
    private Long id;
    private String profileName;
    @JsonIgnore
    @ManyToOne
    private InstrumentRoot instrumentRoot;

    @JsonProperty
    public Long getInstrumentRootId() {
        return instrumentRoot.getId();
    }

    // TODO
    // market data settings
    // order settings
    // risk settings


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Settings that = (Settings) o;

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

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public InstrumentRoot getInstrumentRoot() {
        return instrumentRoot;
    }

    public void setInstrumentRoot(InstrumentRoot instrumentRoot) {
        this.instrumentRoot = instrumentRoot;
    }
}
