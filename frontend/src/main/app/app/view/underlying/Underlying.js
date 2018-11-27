/**
 * Created by robertk on 11/27/2018.
 */
Ext.define('HopGui.view.underlying.Underlying', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'HopGui.view.underlying.UnderlyingController',
        'HopGui.view.underlying.UnderlyingModel',
        'HopGui.view.underlying.UnderlyingGrid'
    ],

    xtype: 'hop-underlying',
    header: false,
    border: false,
    controller: 'hop-underlying',
    viewModel: {
        type: 'hop-underlying'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    items: [{
        xtype: 'hop-underlying-grid',
        reference: 'underlyingGrid'
    }]
});