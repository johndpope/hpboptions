package com.highpowerbear.hpboptions.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.highpowerbear.hpboptions.common.HopSettings;

import java.time.LocalDate;

/**
 * Created by robertk on 1/10/2019.
 */
public class ChainActivationResult {

    private final boolean success;
    private final Integer underlyingConid;
    private final String underlyingSymbol;
    @JsonFormat(pattern = HopSettings.JSON_DATE_FORMAT)
    private final LocalDate expiration;

    public ChainActivationResult(boolean success, Integer underlyingConid, String underlyingSymbol, LocalDate expiration) {
        this.success = success;
        this.underlyingConid = underlyingConid;
        this.underlyingSymbol = underlyingSymbol;
        this.expiration = expiration;
    }

    public boolean isSuccess() {
        return success;
    }

    public Integer getUnderlyingConid() {
        return underlyingConid;
    }

    public String getUnderlyingSymbol() {
        return underlyingSymbol;
    }

    public LocalDate getExpiration() {
        return expiration;
    }
}
