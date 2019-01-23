/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.OrderDataHolder', {
    extend: 'HopGui.model.DataHolderBase',

    fields: [
        {name: 'right', mapping: 'instrument.right'},
        {name: 'strike', mapping: 'instrument.strike'},
        {name: 'expiration', type: 'date', dateFormat: 'Y-m-d', mapping: 'instrument.expiration'},
        {name: 'orderId', mapping: 'hopOrder.orderId'},
        {name: 'action', mapping: 'hopOrder.action'},
        {name: 'orderType', mapping: 'hopOrder.orderType'},
        {name: 'permId', mapping: 'hopOrder.permId'},
        {name: 'quantity', mapping: 'hopOrder.quantity'},
        {name: 'limitPrice', mapping: 'hopOrder.limitPrice'},
        {name: 'chase', mapping: 'hopOrder.chase'},
        {name: 'fillPrice', mapping: 'hopOrder.fillPrice'},
        {name: 'ibStatus', mapping: 'hopOrder.ibStatus'},
        {name: 'state', mapping: 'hopOrder.state'},
        {name: 'heartbeatCount', mapping: 'hopOrder.heartbeatCount'}
    ]
});