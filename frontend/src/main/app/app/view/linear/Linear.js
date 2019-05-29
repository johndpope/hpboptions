/**
 * Created by robertk on 5/29/2019.
 */
Ext.define('HopGui.view.linear.Linear', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'HopGui.view.linear.LinearController',
        'HopGui.view.linear.LinearModel',
        'HopGui.view.linear.LinearGrid'
    ],

    xtype: 'hop-linear',
    header: false,
    border: false,
    controller: 'hop-linear',
    viewModel: {
        type: 'hop-linear'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    items: [{
        xtype: 'hop-linear-grid',
        reference: 'linearGrid'
    }]
});
