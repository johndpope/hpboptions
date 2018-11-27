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
    }, {
        text: 'Sec',
        width: 60,
        dataIndex: 'secType'
    }, {
        text: 'Symbol',
        width: 180,
        dataIndex: 'symbol'
    }, {
        text: 'Currency',
        width: 120,
        dataIndex: 'currency'
    }, {
        text: 'Exchange',
        width: 120,
        dataIndex: 'exchange'
    }, {
        text: 'Bid Size',
        width: 100,
        dataIndex: 'bidSize',
        align: 'right'
    }, {
        text: 'Bid',
        width: 100,
        dataIndex: 'bid',
        align: 'right'
    }, {
        text: 'Ask',
        width: 100,
        dataIndex: 'ask',
        align: 'right'
    }, {
        text: 'Ask Size',
        width: 100,
        dataIndex: 'askSize',
        align: 'right'
    }, {
        text: 'Last',
        width: 100,
        dataIndex: 'last',
        align: 'right'
    }, {
        text: 'Last Size',
        width: 100,
        dataIndex: 'lastSize',
        align: 'right'
    }, {
        text: 'Volume',
        width: 100,
        dataIndex: 'volume',
        align: 'right'
    }, {
        text: 'Close',
        width: 100,
        dataIndex: 'close',
        align: 'right'
    }, {
        text: 'Change',
        width: 100,
        dataIndex: 'changePct',
        align: 'right'
    }, {
        flex: 1
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{underlyings}',
        dock: 'bottom',
        displayInfo: true
    }]
});