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
        'HopGui.view.underlying.Underlying',
        'HopGui.view.order.Order',
        'HopGui.view.position.Position',
        'HopGui.view.chain.Chain'
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
    items: [{
        xtype: 'hop-underlying',
        reference: 'underlyingPanel'
    }, {
        xtype: 'hop-order',
        reference: 'orderPanel'
    }, {
        xtype: 'tabpanel',
        listeners: {
            beforerender: 'setGlyphs'
        },
        items: [{
            xtype: 'hop-position',
            reference: 'positionPanel'
        }, {
            xtype: 'hop-chain',
            reference: 'chainPanel'
        }]
    }]
});
