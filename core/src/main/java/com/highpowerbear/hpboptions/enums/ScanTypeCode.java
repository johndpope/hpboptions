package com.highpowerbear.hpboptions.enums;

/**
 * Created by robertk on 5/13/2019.
 */
public enum ScanTypeCode {
    OPT_VOLUME_MOST_ACTIVE("Most Active By Opt Volume"),
    OPT_OPEN_INTEREST_MOST_ACTIVE("Most Active By Opt Open Interest");

    private String displayName;

    ScanTypeCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return name();
    }
}
