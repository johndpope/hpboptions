/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.PositionDataHolder', {
    extend: 'HopGui.model.MarketDataHolderBase',

    fields: [
        {name: 'right', mapping: 'instrument.right'},
        {name: 'strike', mapping: 'instrument.strike'},
        {name: 'expiration', type: 'date', dateFormat: 'Y-m-d', mapping: 'instrument.expiration'},
        'daysToExpiration',
        'positionSize',
        'unrealizedPnl',
        'margin',
        'intrinsicValue',
        'timeValue',
        'timeValuePct',
        'delta',
        'gamma',
        'impliedVol',
        'optionOpenInterest'
    ]
});