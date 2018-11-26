/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-order-grid',
    requires: [
        'Ext.grid.column.Date',
        'HopGui.view.order.OrderController',
        'HopGui.view.order.OrderModel'
    ],
    title: 'Orders',
    controller: 'hop-order',
    viewModel: {
        type: 'hop-order'
    },
    reference: 'orderGrid',
    bind: '{orders}',
    listeners: {
        'cellclick': 'submitOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'ID',
        width: 80,
        dataIndex: 'id'
    }]
});