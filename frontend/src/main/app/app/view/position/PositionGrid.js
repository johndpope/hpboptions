/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-position-grid',
    requires: [
        'Ext.grid.column.Date',
        'HopGui.view.position.PositionController',
        'HopGui.view.position.PositionModel'
    ],
    title: 'Positions',
    controller: 'hop-position',
    viewModel: {
        type: 'hop-position'
    },
    reference: 'positionGrid',
    bind: '{positions}',
    listeners: {
        'cellclick': 'placeOrder'
    },
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: 'ID',
        width: 80,
        dataIndex: 'id'
    }]
});