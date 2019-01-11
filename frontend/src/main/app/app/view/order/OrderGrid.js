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
    bind: '{orderDataHolders}',
    listeners: {
        cellclick: 'submitOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'Sec',
        width: 60,
        dataIndex: 'secType'
    }, {
        text: 'Und',
        width: 60,
        dataIndex: 'underlyingSymbol'
    }, {
        text: 'Symbol',
        width: 180,
        dataIndex: 'symbol'
    }, {
        text: 'Expiration',
        width: 100,
        dataIndex: 'expiration',
        xtype: 'datecolumn',
        format: 'm/d/Y'
    }, {
        text: 'R',
        width: 30,
        dataIndex: 'right',
        renderer: function(val, metadata, record) {
            return (val.charAt(0));
        }
    }, {
        text: 'S',
        width: 50,
        dataIndex: 'strike',
        align: 'right'
    }, {
        text: 'Bs',
        width: 60,
        dataIndex: 'bidSize',
        tdCls: 'bidSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Bid',
        width: 80,
        dataIndex: 'bid',
        tdCls: 'bid hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 80,
        dataIndex: 'ask',
        tdCls: 'ask hop-price hop-unchanged',
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
        width: 80,
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
        flex: 1,
        menuDisabled: true
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{orderDataHolders}',
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