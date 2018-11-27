/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-underlying-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging'
    ],
    bind: '{underlyings}',
    listeners: {
        'cellclick': 'setupChain'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'ReqID',
        width: 80,
        dataIndex: 'ibRequestId'
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{underlyings}',
        dock: 'bottom',
        displayInfo: true
    }]
});