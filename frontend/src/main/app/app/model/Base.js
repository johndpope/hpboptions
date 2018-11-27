/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.Base', {
    extend: 'Ext.data.Model',

    idProperty: 'ibRequestId',
    fields: [
        {name: 'ibRequestId', type: 'int'}
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