/**
 * Created by robertk on 11/27/2018.
 */
Ext.define('HopGui.view.position.Position', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'HopGui.view.position.PositionController',
        'HopGui.view.position.PositionModel',
        'HopGui.view.position.PositionGrid'
    ],
    title: 'Positions',
    xtype: 'hop-position',
    header: false,
    border: false,
    controller: 'hop-position',
    viewModel: {
        type: 'hop-position'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    items: [{
        xtype: 'hop-position-grid',
        reference: 'positionGrid'
    }]
});