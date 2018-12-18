/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.PositionDataHolder', {
    extend: 'HopGui.model.DataHolderBase',

    fields: [
        {name: 'right', mapping: 'instrument.right'},
        {name: 'strike', mapping: 'instrument.strike'},
        {name: 'expirationDate', type: 'date', dateFormat: 'Y-m-d', mapping: 'instrument.expirationDate'},
        'daysToExpiration',
        'positionSize',
        'unrealizedPnl',
        'timeValue',
        'timeValuePct',
        'delta',
        'gamma',
        'impliedVol',
        'optionOpenInterest'
    ]
});