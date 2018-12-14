/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.PositionDataHolder', {
    extend: 'HopGui.model.DataHolderBase',

    fields: [
        'right',
        'strike',
        {name: 'expirationDate', type: 'date', dateFormat: 'Y-m-d'},
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