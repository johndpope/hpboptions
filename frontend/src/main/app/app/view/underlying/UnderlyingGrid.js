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
        width: 70,
        dataIndex: 'ibRequestId'
    }, {
        text: 'Sec',
        width: 60,
        dataIndex: 'secType'
    }, {
        text: 'Sym',
        width: 60,
        dataIndex: 'symbol'
    }, {
        text: 'Cur',
        width: 60,
        dataIndex: 'currency'
    }, {
        text: 'Exchange',
        width: 100,
        dataIndex: 'exchange'
    }, {
        text: 'BidS',
        width: 80,
        dataIndex: 'bidSize',
        tdCls: 'bidSize hop-size',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Bid',
        width: 80,
        dataIndex: 'bid',
        tdCls: 'bid hop-price',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 80,
        dataIndex: 'ask',
        tdCls: 'ask hop-price',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'AskS',
        width: 80,
        dataIndex: 'askSize',
        tdCls: 'askSize hop-size',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Last',
        width: 80,
        dataIndex: 'last',
        tdCls: 'last hop-price',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'LastS',
        width: 80,
        dataIndex: 'lastSize',
        tdCls: 'lastSize hop-size',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Vol',
        width: 80,
        dataIndex: 'volume',
        tdCls: 'volume hop-size',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Close',
        width: 80,
        dataIndex: 'close',
        tdCls: 'close hop-price',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Chg%',
        width: 80,
        dataIndex: 'changePct',
        tdCls: 'changePct hop-pct',
        align: 'right',
        renderer: 'pctRenderer'
    }, {
        flex: 1
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{underlyings}',
        dock: 'bottom',
        displayInfo: true
    }, {
        xtype: 'toolbar',
        items: [{
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Connect',
            handler: 'connect',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('play'));
                }
            }
        }, {
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Disconnect',
            handler: 'disconnect',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('stop'));
                }
            }
        }, {
            xtype: 'tbtext',
            html: 'connection info',
            width: 180,
            margin: '0 0 0 10',
            reference: 'ibConnectionInfo'
        }]
    }]
});