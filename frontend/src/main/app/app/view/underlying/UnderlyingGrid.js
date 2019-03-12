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
    listeners: {
        cellclick: 'createOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'Underlying',
        width: 115,
        dataIndex: 'secType',
        renderer: function(val, metadata, record) {
            return '<span style="font-weight: bold;">' + record.data['symbol'] + '</span>' + '&nbsp;&nbsp;' + record.data['secType'] + ',' + record.data['currency'];
        }
    }, {
        text: 'Dh',
        width: 45,
        dataIndex: 'deltaHedge',
        xtype: 'checkcolumn',
        listeners: {
            checkchange: 'toggleDeltaHedge'
        }
    }, {
        text: 'P',
        width: 50,
        dataIndex: 'cfdPositionSize',
        tdCls: 'cfdPositionSize hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'Pl',
        width: 50,
        dataIndex: 'cfdUnrealizedPnl',
        tdCls: 'cfdUnrealizedPnl hop-pnl hop-unchanged',
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
        text: 'OptIV',
        width: 70,
        dataIndex: 'optionImpliedVol',
        tdCls: 'optionImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'IvCh',
        width: 65,
        dataIndex: 'ivChangePct',
        tdCls: 'ivChangePct hop-iv-change-pct',
        align: 'right',
        renderer: 'ivChangePctRenderer'
    }, {
        text: 'IvRk',
        width: 65,
        dataIndex: 'ivRank',
        tdCls: 'ivRank hop-iv-rank hop-unchanged',
        align: 'right',
        renderer: 'ivRankRenderer'
    }, {
        text: 'OptVm',
        width: 75,
        dataIndex: 'optionVolume',
        tdCls: 'optionVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'OptOI',
        width: 75,
        dataIndex: 'optionOpenInterest',
        tdCls: 'optionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'p',
        width: 40,
        dataIndex: 'putsShort',
        tdCls: 'putsShort hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'p',
        width: 40,
        dataIndex: 'putsLong',
        tdCls: 'putsLong hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'c',
        width: 40,
        dataIndex: 'callsShort',
        tdCls: 'callsShort hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'c',
        width: 40,
        dataIndex: 'callsLong',
        tdCls: 'callsLong hop-position',
        align: 'right',
        renderer: 'positionRenderer'
    }, {
        text: 'D1p',
        width: 60,
        dataIndex: 'portfolioDeltaOnePct',
        tdCls: 'portfolioDeltaOnePct hop-decimal-one',
        align: 'right',
        renderer: 'decimalOneRenderer'
    }, {
        text: 'G1p',
        width: 60,
        dataIndex: 'portfolioGammaOnePctPct',
        tdCls: 'portfolioGammaOnePctPct hop-decimal-one',
        align: 'right',
        renderer: 'decimalOneRenderer'
    }, {
        text: 'D',
        width: 50,
        dataIndex: 'portfolioDelta',
        tdCls: 'portfolioDelta hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'G',
        width: 50,
        dataIndex: 'portfolioGamma',
        tdCls: 'portfolioGamma hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'V',
        width: 50,
        dataIndex: 'portfolioVega',
        tdCls: 'portfolioVega hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'T',
        width: 50,
        dataIndex: 'portfolioTheta',
        tdCls: 'portfolioTheta hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'Tv',
        width: 50,
        dataIndex: 'portfolioTimeValue',
        tdCls: 'portfolioTimeValue hop-whole',
        align: 'right',
        renderer: 'wholeRenderer'
    }, {
        text: 'PnL',
        width: 60,
        dataIndex: 'portfolioUnrealizedPnl',
        tdCls: 'portfolioUnrealizedPnl hop-pnl hop-unchanged',
        align: 'right',
        renderer: 'pnlRenderer'
    }, {
        text: 'Allc',
        width: 60,
        dataIndex: 'allocationPct',
        tdCls: 'allocationPct hop-decimal-pct hop-unchanged',
        align: 'right',
        renderer: 'decimalPctRenderer'
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
            handler: 'refreshAccountData',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('refresh'));
                }
            }
        }, {
            xtype: 'tbtext',
            html: 'Account data N/A',
            width: 500,
            margin: '0 0 0 10',
            reference: 'accountData'
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