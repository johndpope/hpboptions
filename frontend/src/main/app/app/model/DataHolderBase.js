/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.DataHolderBase', {
    extend: 'Ext.data.Model',

    idProperty: 'id',
    fields: [
        'id',
        'instrument',
        {name: 'secType', mapping: 'instrument.secType'},
        {name: 'symbol', mapping: 'instrument.symbol'},
        {name: 'underlying', mapping: 'instrument.underlying'},
        {name: 'currency', mapping: 'instrument.currency'},
        {name: 'exchange', mapping: 'instrument.exchange'},
        'ibMktDataRequestId',
        'bid',
        'ask',
        'last',
        'close',
        'changePct',
        'bidSize',
        'askSize',
        'lastSize',
        'volume'
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