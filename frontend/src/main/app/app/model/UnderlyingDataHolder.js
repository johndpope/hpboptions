/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.UnderlyingDataHolder', {
    extend: 'HopGui.model.DataHolderBase',

    fields: [
        'ibHistDataRequestId',
        'optionImpliedVol',
        'ivClose',
        'ivChangePct',
        'ivRank',
        'optionVolume',
        'optionOpenInterest'
    ]
});