/**
 * Created by robertk on 5/29/2019.
 */
Ext.define('HopGui.model.LinearDataHolder', {
    extend: 'HopGui.model.MarketDataHolderBase',

    fields: [
        'ibPnlRequestId',
        'positionSize',
        'unrealizedPnl'
    ]
});
