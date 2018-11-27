/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-order-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging'
    ],
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
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{orders}',
        dock: 'bottom',
        displayInfo: true
    }]
});