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
        cellclick: 'placeOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'TV',
        width: 60,
        dataIndex: 'callTimeValue',
        tdCls: 'callTimeValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'TPct',
        width: 70,
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
        width: 80,
        dataIndex: 'callBid',
        tdCls: 'callBid hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 80,
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
        text: 'Last',
        width: 80,
        dataIndex: 'callLast',
        tdCls: 'callLast hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ls',
        width: 60,
        dataIndex: 'callLastSize',
        tdCls: 'callLastSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Volume',
        width: 80,
        dataIndex: 'callVolume',
        tdCls: 'callVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Close',
        width: 80,
        dataIndex: 'callClose',
        tdCls: 'callClose hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Change',
        width: 80,
        dataIndex: 'callChangePct',
        tdCls: 'callChangePct hop-change-pct',
        align: 'right',
        renderer: 'changePctRenderer'
    }, {
        text: 'ImpVol',
        width: 80,
        dataIndex: 'callImpliedVol',
        tdCls: 'callImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'OpInt',
        width: 80,
        dataIndex: 'callOptionOpenInterest',
        tdCls: 'callOptionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Strike',
        width: 80,
        dataIndex: 'strike'
    }, {
        text: 'TV',
        width: 60,
        dataIndex: 'putTimeValue',
        tdCls: 'putTimeValue hop-decimal hop-unchanged',
        align: 'right',
        renderer: 'decimalRenderer'
    }, {
        text: 'TPct',
        width: 70,
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
        width: 80,
        dataIndex: 'putBid',
        tdCls: 'putBid hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ask',
        width: 80,
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
        text: 'Last',
        width: 80,
        dataIndex: 'putLast',
        tdCls: 'putLast hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Ls',
        width: 60,
        dataIndex: 'putLastSize',
        tdCls: 'putLastSize hop-size hop-unchanged',
        align: 'right',
        renderer: 'sizeRenderer'
    }, {
        text: 'Volume',
        width: 80,
        dataIndex: 'putVolume',
        tdCls: 'putVolume hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }, {
        text: 'Close',
        width: 80,
        dataIndex: 'putClose',
        tdCls: 'putClose hop-price hop-unchanged',
        align: 'right',
        renderer: 'priceRenderer'
    }, {
        text: 'Change',
        width: 80,
        dataIndex: 'putChangePct',
        tdCls: 'putChangePct hop-change-pct',
        align: 'right',
        renderer: 'changePctRenderer'
    }, {
        text: 'ImpVol',
        width: 80,
        dataIndex: 'putImpliedVol',
        tdCls: 'putImpliedVol hop-iv hop-unchanged',
        align: 'right',
        renderer: 'ivRenderer'
    }, {
        text: 'OpInt',
        width: 80,
        dataIndex: 'putOptionOpenInterest',
        tdCls: 'putOptionOpenInterest hop-volume hop-unchanged',
        align: 'right',
        renderer: 'volumeRenderer'
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        bind: '{chainItems}',
        dock: 'bottom',
        displayInfo: true
        // TODO underlyings, expirations combo boxes
    }]
});