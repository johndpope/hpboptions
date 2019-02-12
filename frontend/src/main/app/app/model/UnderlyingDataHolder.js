/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.UnderlyingDataHolder', {
    extend: 'HopGui.model.MarketDataHolderBase',

    fields: [
        'ibHistDataRequestId',
        'optionImpliedVol',
        'cfdPositionSize',
        'cfdUnrealizedPnl',
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
        'portfolioUnrealizedPnl',
        'allocationPct'
    ]
});