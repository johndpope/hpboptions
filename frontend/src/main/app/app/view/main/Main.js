/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.main.Main', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'Ext.tab.Panel',
        'HopGui.common.Glyphs',
        'HopGui.view.main.MainController',
        'HopGui.view.main.MainModel',
        'HopGui.view.underlying.UnderlyingGrid',
        'HopGui.view.order.OrderGrid',
        'HopGui.view.position.PositionGrid',
        'HopGui.view.chain.ChainGrid'
    ],
    
    controller: 'main',
    viewModel: {
        type: 'main'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    listeners: {
        beforerender: 'setGlyphs'
    },
    items: [{
        xtype: 'hop-underlying-grid'
    }, {
        xtype: 'hop-order-grid'
    }, {
        xtype: 'tabpanel',
        listeners: {
            beforerender: 'setGlyphs'
        },
        items: [{
            xtype: 'hop-position-grid'
        }, {
            xtype: 'hop-chain-grid'
        }]
    }]
});
