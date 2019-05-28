package com.highpowerbear.hpboptions.dataholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.highpowerbear.hpboptions.enums.DataHolderType;
import com.highpowerbear.hpboptions.field.*;
import com.highpowerbear.hpboptions.model.Instrument;

import java.time.LocalDate;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by robertk on 5/16/2019.
 */
public abstract class AbstractUnderlyingDataHolder extends AbstractMarketDataHolder implements UnderlyingDataHolder {

    private final int ibHistDataRequestId;
    private final TreeMap<LocalDate, Double> ivHistoryMap = new TreeMap<>();
    @JsonIgnore
    private final Set<DataField> ivHistoryDependentFields;

    public AbstractUnderlyingDataHolder(DataHolderType type, Instrument instrument, int ibMktDataRequestId, int ibHistDataRequestId) {
        super(type, instrument, ibMktDataRequestId);
        this.ibHistDataRequestId = ibHistDataRequestId;

        UnderlyingDataField.fields().forEach(field -> valueMap.put(field, createValueQueue(field.getInitialValue())));

        addFieldsToDisplay(Stream.of(
                DerivedMarketDataField.CHANGE_PCT,
                BasicMarketDataField.OPTION_IMPLIED_VOL,
                DerivedMarketDataField.IV_CHANGE_PCT,
                DerivedMarketDataField.IV_RANK,
                DerivedMarketDataField.OPTION_VOLUME,
                DerivedMarketDataField.OPTION_OPEN_INTEREST
        ).collect(Collectors.toSet()));

        ivHistoryDependentFields = Stream.of(
                UnderlyingDataField.IV_CLOSE,
                DerivedMarketDataField.IV_CHANGE_PCT,
                DerivedMarketDataField.IV_RANK
        ).collect(Collectors.toSet());
    }

    @Override
    public void calculateField(DerivedMarketDataField field) {
        super.calculateField(field);

        if (field == DerivedMarketDataField.IV_CHANGE_PCT) {
            updateIvChangePct();

        } else if (field == DerivedMarketDataField.IV_RANK) {
            updateIvRank();

        } else if (field == DerivedMarketDataField.OPTION_VOLUME) {
            int o = getCurrent(BasicMarketDataField.OPTION_CALL_VOLUME).intValue();
            int p = getCurrent(BasicMarketDataField.OPTION_PUT_VOLUME).intValue();

            int value = (isValidSize(o) ? o : 0) + (isValidSize(p) ? p : 0);
            update(field, value);
        }
    }

    @Override
    public void addImpliedVolatility(LocalDate date, double impliedVolatility) {
        ivHistoryMap.putIfAbsent(date, impliedVolatility);
    }

    @Override
    public void impliedVolatilityHistoryCompleted() {
        if (!ivHistoryMap.isEmpty()) {
            update(UnderlyingDataField.IV_CLOSE, ivHistoryMap.lastEntry().getValue());

            updateIvChangePct();
            updateIvRank();
        }
    }

    private void updateIvChangePct() {
        if (ivHistoryMap.isEmpty()) {
            return;
        }
        double ivCurrent = getCurrent(BasicMarketDataField.OPTION_IMPLIED_VOL).doubleValue();
        double ivClose = ivHistoryMap.lastEntry().getValue();

        if (isValidPrice(ivCurrent) && isValidPrice(ivClose)) {
            double value = ((ivCurrent - ivClose) / ivClose) * 100d;
            update(DerivedMarketDataField.IV_CHANGE_PCT, value);
        }
    }

    private void updateIvRank() {
        if (ivHistoryMap.isEmpty()) {
            return;
        }
        LocalDate now = LocalDate.now();
        LocalDate yearAgo = now.minusYears(1);

        OptionalDouble ivYearLowOptional = ivHistoryMap.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(yearAgo))
                .mapToDouble(Map.Entry::getValue)
                .min();

        OptionalDouble ivYearHighOptional = ivHistoryMap.entrySet().stream()
                .filter(entry -> entry.getKey().isAfter(yearAgo))
                .mapToDouble(Map.Entry::getValue)
                .max();

        if (!ivYearLowOptional.isPresent() || !ivYearHighOptional.isPresent()) {
            return;
        }

        double ivCurrent = getOptionImpliedVol();
        double ivYearLow = ivYearLowOptional.getAsDouble();
        double ivYearHigh = ivYearHighOptional.getAsDouble();

        if (isValidPrice(ivCurrent) && isValidPrice(ivYearLow) && isValidPrice(ivYearHigh)) {
            double ivRank = 100d * (ivCurrent - ivYearLow) / (ivYearHigh - ivYearLow);
            update(DerivedMarketDataField.IV_RANK, ivRank);
        }
    }

    @Override
    public Set<DataField> getIvHistoryDependentFields() {
        return ivHistoryDependentFields;
    }

    public int getIbHistDataRequestId() {
        return ibHistDataRequestId;
    }

    public double getOptionImpliedVol() {
        return getCurrent(BasicMarketDataField.OPTION_IMPLIED_VOL).doubleValue();
    }

    public double getIvClose() {
        return getCurrent(UnderlyingDataField.IV_CLOSE).doubleValue();
    }
}
