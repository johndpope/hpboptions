package com.highpowerbear.hpboptions.field;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 6/6/2019.
 */
public enum AccountDataField implements DataField {
    NET_LIQUIDATION_VALUE,
    AVAILABLE_FUNDS,
    UNREALIZED_PNL;

    private static List<AccountDataField> fields = Arrays.asList(AccountDataField.values());

    public static List<AccountDataField> fields() {
        return fields;
    }

    private static List<AccountDataField> accountSummaryFields = Stream.of(
            NET_LIQUIDATION_VALUE,
            AVAILABLE_FUNDS
    ).collect(Collectors.toList());

    public static List<AccountDataField> accountSummaryFields() {
        return accountSummaryFields;
    }
}
