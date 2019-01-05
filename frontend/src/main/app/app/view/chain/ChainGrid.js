/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-chain-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging',
        'Ext.form.field.ComboBox'
    ],
    bind: '{activeChainItems}',
    listeners: {
        cellclick: 'placeOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'Tv',
        width: 60,
        dataIndex: 'callTimeValue',
        tdCls: 'callTimeValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Tpct',
        width: 75,
        dataIndex: 'callTimeValuePct',
        tdCls: 'callTimeValuePct hop-decimal-pct hop-unchanged',
        align: 'right',
        renderer: 'decimalPctRenderer'
    }, {
        text: 'Dlt',
        width: 60,
        dataIndex: 'callDelta',
        tdCls: 'callDelta hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Bs',
        width: 60,
        dataIndex: 'callBidSize',
        tdCls: 'callBidSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Bid',
        width: 65,
        dataIndex: 'callBid',
        tdCls: 'callBid hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 65,
        dataIndex: 'callAsk',
        tdCls: 'callAsk hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'As',
        width: 60,
        dataIndex: 'callAskSize',
        tdCls: 'callAskSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Lst',
        width: 65,
        dataIndex: 'callLast',
        tdCls: 'callLast hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ls',
        width: 55,
        dataIndex: 'callLastSize',
        tdCls: 'callLastSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Vlm',
        width: 65,
        dataIndex: 'callVolume',
        tdCls: 'callVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Cls',
        width: 65,
        dataIndex: 'callClose',
        tdCls: 'callClose hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Chg',
        width: 65,
        dataIndex: 'callChange',
        tdCls: 'callChange hop-change',
        align: 'right',
        renderer: 'changeRenderer'
    }, {
        text: 'IV',
        width: 60,
        dataIndex: 'callImpliedVol',
        tdCls: 'callImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'OI',
        width: 60,
        dataIndex: 'callOptionOpenInterest',
        tdCls: 'callOptionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Strike',
        width: 70,
        dataIndex: 'strike',
        tdCls: 'hop-chain-strike',
        align: 'center'
    }, {
        text: 'Tv',
        width: 60,
        dataIndex: 'putTimeValue',
        tdCls: 'putTimeValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Tpct',
        width: 75,
        dataIndex: 'putTimeValuePct',
        tdCls: 'putTimeValuePct hop-decimal-pct hop-unchanged',
        align: 'right',
        renderer: 'decimalPctRenderer'
    }, {
        text: 'Dlt',
        width: 60,
        dataIndex: 'putDelta',
        tdCls: 'putDelta hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'Bs',
        width: 60,
        dataIndex: 'putBidSize',
        tdCls: 'putBidSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Bid',
        width: 65,
        dataIndex: 'putBid',
        tdCls: 'putBid hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 65,
        dataIndex: 'putAsk',
        tdCls: 'putAsk hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'As',
        width: 60,
        dataIndex: 'putAskSize',
        tdCls: 'putAskSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Lst',
        width: 65,
        dataIndex: 'putLast',
        tdCls: 'putLast hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ls',
        width: 55,
        dataIndex: 'putLastSize',
        tdCls: 'putLastSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Vlm',
        width: 65,
        dataIndex: 'putVolume',
        tdCls: 'putVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Cls',
        width: 65,
        dataIndex: 'putClose',
        tdCls: 'putClose hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Chg',
        width: 65,
        dataIndex: 'putChange',
        tdCls: 'putChange hop-change',
        align: 'right',
        renderer: 'changeRenderer'
    }, {
        text: 'IV',
        width: 60,
        dataIndex: 'putImpliedVol',
        tdCls: 'putImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'OI',
        width: 60,
        dataIndex: 'putOptionOpenInterest',
        tdCls: 'putOptionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        flex: 1,
        menuDisabled: true
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{activeChainItems}',
        dock: 'bottom',
        displayInfo: true
    }, {
        xtype: 'toolbar',
        items: [{
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'symbol',
            valueField: 'conid',
            reference: 'underlyingCombo',
            fieldLabel: 'Underlying',
            width: 150,
            labelWidth: 65,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['symbol', 'conid'],
                data: [
                    {"symbol": "symbol", "conid": "conid"}
                ]
            }),
            margin: '0 0 0 10',
            listeners: {
                change: 'prepareExpirationCombo'
            }
        }, {
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'formattedDate',
            valueField: 'date',
            reference: 'expirationCombo',
            fieldLabel: 'Expiration',
            width: 180,
            labelWidth: 60,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['formattedDate', 'date'],
                data: [
                    {"formattedDate": "formattedDate", "date": "date"}
                ]
            }),
            margin: '0 0 0 10'
        }, {
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Load',
            handler: 'loadChain',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('download'));
                }
            }
        }, {
            xtype: 'tbtext',
            html: 'Chain status N/A',
            width: 150,
            margin: '0 0 0 10',
            reference: 'chainStatus'
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