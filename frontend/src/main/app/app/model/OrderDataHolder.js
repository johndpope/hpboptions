/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.OrderDataHolder', {
    extend: 'HopGui.model.MarketDataHolderBase',

    fields: [
        {name: 'right', mapping: 'instrument.right'},
        {name: 'strike', mapping: 'instrument.strike'},
        {name: 'orderId', mapping: 'hopOrder.orderId'},
        {name: 'action', mapping: 'hopOrder.action'},
        {name: 'orderType', mapping: 'hopOrder.orderType'},
        {name: 'orderSource', mapping: 'hopOrder.orderSource'},
        {name: 'permId', mapping: 'hopOrder.permId'},
        {name: 'quantity', mapping: 'hopOrder.quantity'},
        {name: 'limitPrice', mapping: 'hopOrder.limitPrice'},
        {name: 'adapt', mapping: 'hopOrder.adapt'},
        {name: 'fillPrice', mapping: 'hopOrder.fillPrice'},
        {name: 'ibStatus', mapping: 'hopOrder.ibStatus'},
        {name: 'state', mapping: 'hopOrder.state'},
        {name: 'heartbeatCount', mapping: 'hopOrder.heartbeatCount'}
    ]
});
