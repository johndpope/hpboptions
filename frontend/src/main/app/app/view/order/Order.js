/**
 * Created by robertk on 11/27/2018.
 */
Ext.define('HopGui.view.order.Order', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'HopGui.view.order.OrderController',
        'HopGui.view.order.OrderModel',
        'HopGui.view.order.OrderGrid'
    ],

    xtype: 'hop-order',
    header: false,
    border: false,
    controller: 'hop-order',
    viewModel: {
        type: 'hop-order'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    items: [{
        xtype: 'hop-order-grid',
        reference: 'orderGrid'
    }]
});