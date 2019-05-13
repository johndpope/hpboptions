package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 5/13/2019.
 */
public enum ScanTypeCode {
    OPT_VOLUME_MOST_ACTIVE,
    OPT_OPEN_INTEREST_MOST_ACTIVE;

    public String getCode() {
        return name();
    }
}
