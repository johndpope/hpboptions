/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.UnderlyingDataHolder', {
    extend: 'HopGui.model.DataHolderBase',

    fields: [
        'ibHistDataRequestId',
        'optionImpliedVol',
        'cfdPositionSize',
        'ivClose',
        'putsSum',
        'callsSum',
        'ivChangePct',
        'ivRank',
        'optionVolume',
        'optionOpenInterest',
        'portfolioDelta',
        'portfolioDeltaOnePct',
        'portfolioGamma',
        'portfolioGammaOnePctPct',
        'portfolioVega',
        'portfolioTheta',
        'portfolioTimeValue',
        'unrealizedPnl',
        'allocationPct'
    ]
});