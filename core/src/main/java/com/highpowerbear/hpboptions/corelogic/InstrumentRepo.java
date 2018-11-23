package com.highpowerbear.hpboptions.corelogic;

import com.highpowerbear.hpboptions.corelogic.model.Instrument;
import com.highpowerbear.hpboptions.dao.CoreDao;
import com.highpowerbear.hpboptions.entity.OptionRoot;
import com.highpowerbear.hpboptions.enums.InstrumentGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by robertk on 11/23/2018.
 */
@Component
public class InstrumentRepo {

    private final CoreDao coreDao;
    private final List<Instrument> undlInstruments = new ArrayList<>();

    @Autowired
    public InstrumentRepo(CoreDao coreDao) {
        this.coreDao = coreDao;
    }

    public void refreshUndlInstruments() {
        undlInstruments.clear();
        List<OptionRoot> optionRoots = coreDao.getActiveOptionRoots();
        optionRoots.forEach(or -> {
            Instrument instrument = new Instrument(or.getUndlSecType(), or.getUndlSymbol(), or.getUndlSymbol(), or.getCurrency(), or.getUndlExchange(), InstrumentGroup.UNDERLYING);
            undlInstruments.add(instrument);
        });
    }

    public List<Instrument> getUndlInstruments() {
        return Collections.unmodifiableList(undlInstruments);
    }
}
