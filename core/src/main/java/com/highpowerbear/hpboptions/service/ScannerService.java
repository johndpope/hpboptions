package com.highpowerbear.hpboptions.service;

import com.highpowerbear.hpboptions.connector.IbController;
import com.highpowerbear.hpboptions.enums.ScanInstrumentCode;
import com.highpowerbear.hpboptions.enums.ScanTypeCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by robertk on 5/13/2019.
 */
@Service
public class ScannerService extends AbstractUnderlyingService {

    private ScanInstrumentCode activeInstrumentCode;
    private ScanTypeCode activeTypeCode;

    @Autowired
    public ScannerService(IbController ibController, MessageService messageService) {
        super(ibController, messageService);

        ibController.addConnectionListener(this);
    }

    public void activateScanner(ScanInstrumentCode instrumentCode, ScanTypeCode typeCode) {
        activeInstrumentCode = instrumentCode;
        activeTypeCode = typeCode;

        // TODO
    }

    public ScanInstrumentCode getActiveInstrumentCode() {
        return activeInstrumentCode;
    }

    public ScanTypeCode getActiveTypeCode() {
        return activeTypeCode;
    }
}
