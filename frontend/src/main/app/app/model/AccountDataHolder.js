/**
 * Created by robertk on 2/12/2019.
 */
Ext.define('HopGui.model.AccountDataHolder', {
    extend: 'Ext.data.Model',

    idProperty: 'ibAccount',
    fields: [
        'ibAccount',
        'ibPnlRequestId',
        'baseCurrency',
        'netLiquidationValue',
        'availableFunds',
        'unrealizedPnl'
    ],
    schema: {
        id: 'hopSchema',
        namespace: 'HopGui.model',  // generate auto entityName,
        proxy: {
            type: 'ajax',
            actionMethods: {
                read: 'GET',
                update: 'PUT'
            },
            reader: {
                type: 'json',
                rootProperty: 'items',
                totalProperty: 'total'
            }
        }
    }
});