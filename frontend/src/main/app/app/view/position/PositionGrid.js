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
        cellclick: 'placeOrder'
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
        text: 'D',
        width: 50,
        dataIndex: 'daysToExpiration',
        align: 'right'
    }, {
        text: 'P',
        width: 50,
        dataIndex: 'positionSize',
        tdCls: 'positionSize hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'PnL',
        width: 60,
        dataIndex: 'unrealizedPnl',
        tdCls: 'unrealizedPnl hop-pnl hop-unchanged',
        align: 'right',
        renderer: 'pnlRenderer'
    }, {
        text: 'Mrg',
        width: 60,
        dataIndex: 'margin',
        tdCls: 'margin hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'Itm',
        width: 60,
        dataIndex: 'intrinsicValue',
        tdCls: 'intrinsicValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Tv',
        width: 60,
        dataIndex: 'timeValue',
        tdCls: 'timeValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Tpct',
        width: 70,
        dataIndex: 'timeValuePct',
        tdCls: 'timeValuePct hop-decimal-pct hop-unchanged',
        align: 'right',
        renderer: 'decimalPctRenderer'
    }, {
        text: 'Dlt',
        width: 60,
        dataIndex: 'delta',
        tdCls: 'delta hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
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
        text: 'Volume',
        width: 80,
        dataIndex: 'volume',
        tdCls: 'volume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Close',
        width: 80,
        dataIndex: 'close',
        tdCls: 'close hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Chg',
        width: 70,
        dataIndex: 'change',
        tdCls: 'change hop-change',
        align: 'right',
        renderer: 'changeRenderer'
    }, {
        text: 'ImpVol',
        width: 80,
        dataIndex: 'impliedVol',
        tdCls: 'impliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'OpInt',
        width: 80,
        dataIndex: 'optionOpenInterest',
        tdCls: 'optionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        flex: 1,
        menuDisabled: true
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{positionDataHolders}',
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