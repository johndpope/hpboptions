/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-chain-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging'
    ],
    bind: '{chainDataHolders}',
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
        bind: '{chainDataHolders}',
        dock: 'bottom',
        displayInfo: true
    }]
});