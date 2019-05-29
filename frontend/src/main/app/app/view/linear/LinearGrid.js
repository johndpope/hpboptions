/**
 * Created by robertk on 5/29/2019.
 */
Ext.define('HopGui.view.linear.LinearGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-linear-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging'
    ],
    bind: '{linearDataHolders}',
    listeners: {
        cellclick: 'createOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'Instrument',
        width: 115,
        dataIndex: 'secType',
        renderer: function(val, metadata, record) {
            return '<span style="font-weight: bold;">' + record.data['symbol'] + '</span>' + '&nbsp;&nbsp;' + record.data['secType'] + ',' + record.data['currency'];
        }
    }, {
        text: 'P',
        width: 50,
        dataIndex: 'positionSize',
        tdCls: 'positionSize hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'Pl',
        width: 50,
        dataIndex: 'un1realizedPnl',
        tdCls: 'unrealizedPnl hop-pnl hop-unchanged',
        align: 'right',
        renderer: 'pnlRenderer'
    }, {
        text: 'Bs',
        width: 60,
        dataIndex: 'bidSize',
        tdCls: 'bidSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Bid',
        width: 70,
        dataIndex: 'bid',
        tdCls: 'bid hop-price hop-unchanged hop-pointer',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 70,
        dataIndex: 'ask',
        tdCls: 'ask hop-price hop-unchanged hop-pointer',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'As',
        width: 60,
        dataIndex: 'askSize',
        tdCls: 'askSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Last',
        width: 70,
        dataIndex: 'last',
        tdCls: 'last hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ls',
        width: 60,
        dataIndex: 'lastSize',
        tdCls: 'lastSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Vlm',
        width: 75,
        dataIndex: 'volume',
        tdCls: 'volume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Close',
        width: 70,
        dataIndex: 'close',
        tdCls: 'close hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Chg',
        width: 65,
        dataIndex: 'changePct',
        tdCls: 'changePct hop-change-pct',
        align: 'right',
        renderer: 'changePctRenderer'
    }, {
        flex: 1,
        menuDisabled: true
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{linearDataHolders}',
        dock: 'bottom',
        displayInfo: true
    }, {
        xtype: 'toolbar',
        items: [{
            xtype: 'tbtext',
            flex: 1
        }, {
            xtype: 'tbtext',
            html: 'WS status',
            width: 120,
            margin: '0 0 0 10',
            reference: 'wsStatus'
        }]
    }]
});
