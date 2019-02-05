/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-order-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.form.field.Number',
        'Ext.toolbar.Paging',
        'Ext.grid.plugin.CellEditing',
        'Ext.form.CheckboxGroup',
        'Ext.form.field.Checkbox'
    ],
    bind: '{orderDataHolders}',
    viewConfig: {
        stripeRows: true
    },
    selType: 'cellmodel',
    plugins: [{
        ptype: 'cellediting',
        clicksToEdit: 1
    }],
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
            return val ? (val.charAt(0)) : '&nbsp;';
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
        text: 'OrdId',
        width: 80,
        dataIndex: 'orderId',
        align: 'right'
    }, {
        text: 'Action',
        width: 70,
        dataIndex: 'action'
    }, {
        text: 'Qnt',
        width: 60,
        dataIndex: 'quantity',
        align: 'right',
        tdCls: 'hop-pointer',
        editor: {
            xtype: 'numberfield',
            minValue: 0,
            step: 1,
            allowDecimals: false
        }
    }, {
        text: 'Type',
        width: 60,
        dataIndex: 'orderType'
    }, {
        text: 'Lmt',
        width: 70,
        dataIndex: 'limitPrice',
        align: 'right',
        tdCls: 'hop-pointer',
        editor: {
            xtype: 'numberfield',
            minValue: 0,
            step: 0.01,
            allowDecimals: true
        },
        renderer: 'orderPriceRenderer'
    }, {
        text: 'Adpt',
        width: 60,
        dataIndex: 'adapt',
        xtype: 'checkcolumn',
        editor: {
            xtype: 'checkboxfield'
        }
    }, {
        text: 'Fill',
        width: 60,
        dataIndex: 'fillPrice',
        align: 'right',
        renderer: 'orderPriceRenderer'
    }, {
        text: 'State',
        width: 90,
        dataIndex: 'state'
    }, {
        text: 'IB Status',
        width: 110,
        dataIndex: 'ibStatus',
        renderer: function(val, metadata, record) {
            metadata.style ='background-color: ' + HopGui.common.Definitions.getIbStatusColor(val) + '; color: white;';
            return val ? val : 'NA';
        }
    }, {
        text: 'PermId',
        width: 100,
        dataIndex: 'permId',
        align: 'right'
    }, {
        text: 'Hb',
        width: 50,
        dataIndex: 'heartbeatCount',
        align: 'right'
    }, {
        xtype: 'widgetcolumn',
        width : 50,
        widget: {
            xtype: 'button',
            width: 30,
            tooltip: 'Send Order',
            handler: 'sendOrder',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('send'));
                }
            }
        }
    }, {
        xtype: 'widgetcolumn',
        width : 50,
        widget: {
            xtype: 'button',
            width: 30,
            tooltip: 'Cancel Order',
            handler: 'cancelOrder',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('times'));
                }
            }
        }
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
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Send New',
            handler: 'sendNewOrders',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('send'));
                }
            }
        }, {
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Remove Idle',
            handler: 'removeIdleOrders',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HopGui.common.Glyphs.getGlyph('times'));
                }
            }
        }, {
            xtype: 'checkboxgroup',
            reference: 'orderFilter',
            margin: '0 0 0 20',
            labelWidth: 30,
            fieldLabel: 'Filter',
            items: [
                {boxLabel: 'New', name: 'showNew', width: 50},
                {boxLabel: 'Working', name: 'showWorking', width: 75},
                {boxLabel: 'Completed', name: 'showCompleted', width: 85}
            ],
            listeners: {
                change: 'onOrderFilterChange'
            }
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