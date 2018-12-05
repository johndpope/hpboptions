package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.BasicField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedField;
import com.highpowerbear.hpboptions.enums.Field;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractDataHolder implements DataHolder {

    private final DataHolderType type;
    private final Instrument instrument;
    private final int ibRequestId;
    private final Set<Field> fieldsToDisplay = new HashSet<>();
    private String genericTicks;

    private final Map<Field, Number> oldValueMap = new HashMap<>();
    protected final Map<Field, Number> valueMap = new HashMap<>();

    AbstractDataHolder(DataHolderType type, Instrument instrument, int ibRequestId) {
        this.type = type;
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;

        Arrays.asList(BasicField.values()).forEach(field -> update(field, field.getInitialValue()));
        Arrays.asList(DerivedField.values()).forEach(field -> update(field, field.getInitialValue()));

        Stream.of(
                BasicField.BID,
                BasicField.ASK,
                BasicField.LAST,
                BasicField.CLOSE,
                BasicField.BID_SIZE,
                BasicField.ASK_SIZE,
                BasicField.LAST_SIZE,
                BasicField.VOLUME,
                DerivedField.CHANGE_PCT
        ).forEach(fieldsToDisplay::add);

        determineGenericTicks();
    }

    protected void addFieldsToDisplay(Set<Field> fieldsToDisplay) {
        this.fieldsToDisplay.addAll(fieldsToDisplay);
        determineGenericTicks();
    }

    private void determineGenericTicks() {
        Set<Integer> genericTicksSet = new HashSet<>();

        Arrays.stream(BasicField.values())
                .filter(fieldsToDisplay::contains)
                .map(BasicField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        Arrays.stream(DerivedField.values())
                .filter(fieldsToDisplay::contains)
                .flatMap(derivedField -> derivedField.getDependencies().stream())
                .map(BasicField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        genericTicks = StringUtils.join(genericTicksSet, ",");
    }

    @Override
    public void updateField(BasicField field, Number value) {
        update(field, convert(field, value));
    }

    @Override
    public void calculateField(DerivedField field) {
        if (field == DerivedField.CHANGE_PCT) {
            double l = valueMap.get(BasicField.LAST).doubleValue();
            double c = valueMap.get(BasicField.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                update(field, Double.parseDouble(String.format("%.2f", ((l - c) / c) * 100d)));
            }

        } else if (field == DerivedField.OPTION_VOLUME) {
            int o = valueMap.get(BasicField.OPTION_CALL_VOLUME).intValue();
            int p = valueMap.get(BasicField.OPTION_PUT_VOLUME).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(field, o + p);
            }

        } else if (field == DerivedField.OPTION_OPEN_INTEREST) {
            int o = valueMap.get(BasicField.OPTION_CALL_OPEN_INTEREST).intValue();
            int p = valueMap.get(BasicField.OPTION_PUT_OPEN_INTEREST).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(field, o + p);
            }
        }
    }

    private Number convert(BasicField field, Number value) {
        if (type == DataHolderType.UNDERLYING && field == BasicField.VOLUME && isValidSize(value.intValue())) {
            return value.intValue() * 100;

        } else if (type == DataHolderType.UNDERLYING && field == BasicField.OPTION_IMPLIED_VOL && isValidPrice(value.doubleValue())) {
            return Double.parseDouble(String.format("%.1f", value.doubleValue() * 100d));
        }

        return value;
    }

    private void update(Field field, Number value) {
        oldValueMap.put(field, valueMap.get(field));
        valueMap.put(field, value);
    }

    private boolean isValidPrice(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d) && d > 0d;
    }

    private boolean isValidSize(int i) {
        return i > 0;
    }

    @Override
    public DataHolderType getType() {
        return type;
    }

    @Override
    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public int getIbRequestId() {
        return ibRequestId;
    }

    @Override
    public String createMessage(Field field) {
        return type.name().toLowerCase() + "," + ibRequestId + "," + CoreUtil.toCamelCase(field.name()) + "," + oldValueMap.get(field) + "," + valueMap.get(field);
    }

    @Override
    public boolean isDisplayed(Field field) {
        return fieldsToDisplay.contains(field);
    }

    @Override
    public String getGenericTicks() {
        return genericTicks;
    }

    public double getBid() {
        return valueMap.get(BasicField.BID).doubleValue();
    }

    public double getAsk() {
        return valueMap.get(BasicField.ASK).doubleValue();
    }

    public double getLast() {
        return valueMap.get(BasicField.LAST).doubleValue();
    }

    public double getClose() {
        return valueMap.get(BasicField.CLOSE).doubleValue();
    }

    public int getBidSize() {
        return valueMap.get(BasicField.BID_SIZE).intValue();
    }

    public int getAskSize() {
        return valueMap.get(BasicField.ASK_SIZE).intValue();
    }

    public int getLastSize() {
        return valueMap.get(BasicField.LAST_SIZE).intValue();
    }

    public int getVolume() {
        return valueMap.get(BasicField.VOLUME).intValue();
    }

    public double getChangePct() {
        return valueMap.get(DerivedField.CHANGE_PCT).doubleValue();
    }
}
