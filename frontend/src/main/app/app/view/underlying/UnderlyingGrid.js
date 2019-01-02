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
    bind: '{underlyingDataHolders}',
    viewConfig: {
        stripeRows: true
    },
    columns: [{
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
        text: 'Change',
        width: 80,
        dataIndex: 'changePct',
        tdCls: 'changePct hop-change-pct',
        align: 'right',
        renderer: 'changePctRenderer'
    }, {
        text: 'Opt IV',
        width: 80,
        dataIndex: 'optionImpliedVol',
        tdCls: 'optionImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'IvChg',
        width: 70,
        dataIndex: 'ivChangePct',
        tdCls: 'ivChangePct hop-iv-change-pct',
        align: 'right',
        renderer: 'ivChangePctRenderer'
    }, {
        text: 'IvRnk',
        width: 70,
        dataIndex: 'ivRank',
        tdCls: 'ivRank hop-iv-rank hop-unchanged',
        align: 'right',
        renderer: 'ivRankRenderer'
    }, {
        text: 'Opt Vlm',
        width: 80,
        dataIndex: 'optionVolume',
        tdCls: 'optionVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Opt OI',
        width: 80,
        dataIndex: 'optionOpenInterest',
        tdCls: 'optionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Dlt',
        width: 60,
        dataIndex: 'portfolioDelta',
        tdCls: 'portfolioDelta hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'Ddol',
        width: 70,
        dataIndex: 'portfolioDeltaDollars',
        tdCls: 'portfolioDeltaDollars hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'Gm',
        width: 60,
        dataIndex: 'portfolioGamma',
        tdCls: 'portfolioGamma hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'TV',
        width: 60,
        dataIndex: 'portfolioTimeValue',
        tdCls: 'portfolioTimeValue hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'PnL',
        width: 60,
        dataIndex: 'unrealizedPnl',
        tdCls: 'unrealizedPnl hop-pnl hop-unchanged',
        align: 'right',
        renderer: 'pnlRenderer'
    }, {
        flex: 1,
        menuDisabled: true
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{underlyingDataHolders}',
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
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('times'));
                }
            }
        }, {
            xtype: 'button',
            margin: '0 0 0 10',
            text: '',
            handler: 'refreshIbConnectionInfo',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('refresh'));
                }
            }
        }, {
            xtype: 'tbtext',
            html: 'Connection info N/A',
            width: 180,
            margin: '0 0 0 10',
            reference: 'ibConnectionInfo'
        }, {
            xtype: 'button',
            margin: '0 0 0 20',
            text: '',
            handler: 'refreshAccountSummary',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('refresh'));
                }
            }
        }, {
            xtype: 'tbtext',
            html: 'Account summary N/A',
            width: 500,
            margin: '0 0 0 10',
            reference: 'accountSummary'
        }, {
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