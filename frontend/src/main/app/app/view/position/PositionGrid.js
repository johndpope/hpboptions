/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-position-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging'
    ],
    bind: '{positionDataHolders}',
    listeners: {
        'cellclick': 'placeOrder'
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
        bind: '{positionDataHolders}',
        dock: 'bottom',
        displayInfo: true
    }]
});