package com.highpowerbear.hpboptions.corelogic.model;

import com.highpowerbear.hpboptions.common.CoreUtil;
import com.highpowerbear.hpboptions.enums.BasicMktDataField;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.enums.DerivedMktDataField;
import com.highpowerbear.hpboptions.enums.MktDataField;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by robertk on 11/27/2018.
 */
public abstract class AbstractDataHolder implements DataHolder {

    protected final String id;
    private final DataHolderType type;
    private final Instrument instrument;
    private final int ibRequestId;
    private final Set<MktDataField> fieldsToDisplay = new HashSet<>();
    private String genericTicks;

    private final Map<MktDataField, Number> oldValueMap = new HashMap<>();
    protected final Map<MktDataField, Number> valueMap = new HashMap<>();

    AbstractDataHolder(DataHolderType type, Instrument instrument, int ibRequestId) {
        this.type = type;
        this.instrument = instrument;
        this.ibRequestId = ibRequestId;
        id = type.name().toLowerCase() + "-" + instrument.getSymbol();

        Arrays.asList(BasicMktDataField.values()).forEach(field -> update(field, field.getInitialValue()));
        Arrays.asList(DerivedMktDataField.values()).forEach(field -> update(field, field.getInitialValue()));

        Stream.of(
                BasicMktDataField.BID,
                BasicMktDataField.ASK,
                BasicMktDataField.LAST,
                BasicMktDataField.CLOSE,
                BasicMktDataField.BID_SIZE,
                BasicMktDataField.ASK_SIZE,
                BasicMktDataField.LAST_SIZE,
                BasicMktDataField.VOLUME,
                DerivedMktDataField.CHANGE_PCT
        ).forEach(fieldsToDisplay::add);

        determineGenericTicks();
    }

    protected void addFieldsToDisplay(Set<MktDataField> fieldsToDisplay) {
        this.fieldsToDisplay.addAll(fieldsToDisplay);
        determineGenericTicks();
    }

    private void determineGenericTicks() {
        Set<Integer> genericTicksSet = new HashSet<>();

        Arrays.stream(BasicMktDataField.values())
                .filter(fieldsToDisplay::contains)
                .map(BasicMktDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        Arrays.stream(DerivedMktDataField.values())
                .filter(fieldsToDisplay::contains)
                .flatMap(derivedField -> derivedField.getDependencies().stream())
                .map(BasicMktDataField::getGenericTick)
                .filter(Objects::nonNull)
                .forEach(genericTicksSet::add);

        genericTicks = StringUtils.join(genericTicksSet, ",");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void updateField(BasicMktDataField field, Number value) {
        update(field, convert(field, value));
    }

    @Override
    public void calculateField(DerivedMktDataField field) {
        if (field == DerivedMktDataField.CHANGE_PCT) {
            double l = valueMap.get(BasicMktDataField.LAST).doubleValue();
            double c = valueMap.get(BasicMktDataField.CLOSE).doubleValue();

            if (isValidPrice(l) && isValidPrice(c)) {
                update(field, Double.parseDouble(String.format("%.2f", ((l - c) / c) * 100d)));
            }

        } else if (field == DerivedMktDataField.OPTION_VOLUME) {
            int o = valueMap.get(BasicMktDataField.OPTION_CALL_VOLUME).intValue();
            int p = valueMap.get(BasicMktDataField.OPTION_PUT_VOLUME).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(field, o + p);
            }

        } else if (field == DerivedMktDataField.OPTION_OPEN_INTEREST) {
            int o = valueMap.get(BasicMktDataField.OPTION_CALL_OPEN_INTEREST).intValue();
            int p = valueMap.get(BasicMktDataField.OPTION_PUT_OPEN_INTEREST).intValue();

            if (isValidSize(o) && isValidSize(p)) {
                update(field, o + p);
            }
        }
    }

    private Number convert(BasicMktDataField field, Number value) {
        if (type == DataHolderType.UNDERLYING && field == BasicMktDataField.VOLUME && isValidSize(value.intValue())) {
            return value.intValue() * 100;

        } else if (type == DataHolderType.UNDERLYING && field == BasicMktDataField.OPTION_IMPLIED_VOL && isValidPrice(value.doubleValue())) {
            return Double.parseDouble(String.format("%.1f", value.doubleValue() * 100d));
        }

        return value;
    }

    private void update(MktDataField mktDataField, Number value) {
        oldValueMap.put(mktDataField, valueMap.get(mktDataField));
        valueMap.put(mktDataField, value);
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
    public String createMessage(MktDataField mktDataField) {
        return id + "," + CoreUtil.toCamelCase(mktDataField.name()) + "," + oldValueMap.get(mktDataField) + "," + valueMap.get(mktDataField);
    }

    @Override
    public boolean isDisplayed(MktDataField mktDataField) {
        return fieldsToDisplay.contains(mktDataField);
    }

    @Override
    public String getGenericTicks() {
        return genericTicks;
    }

    public double getBid() {
        return valueMap.get(BasicMktDataField.BID).doubleValue();
    }

    public double getAsk() {
        return valueMap.get(BasicMktDataField.ASK).doubleValue();
    }

    public double getLast() {
        return valueMap.get(BasicMktDataField.LAST).doubleValue();
    }

    public double getClose() {
        return valueMap.get(BasicMktDataField.CLOSE).doubleValue();
    }

    public int getBidSize() {
        return valueMap.get(BasicMktDataField.BID_SIZE).intValue();
    }

    public int getAskSize() {
        return valueMap.get(BasicMktDataField.ASK_SIZE).intValue();
    }

    public int getLastSize() {
        return valueMap.get(BasicMktDataField.LAST_SIZE).intValue();
    }

    public int getVolume() {
        return valueMap.get(BasicMktDataField.VOLUME).intValue();
    }

    public double getChangePct() {
        return valueMap.get(DerivedMktDataField.CHANGE_PCT).doubleValue();
    }
}
