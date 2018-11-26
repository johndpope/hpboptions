/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'hop-underlying-grid',
    requires: [
        'Ext.grid.column.Date',
        'HopGui.view.underlying.UnderlyingController',
        'HopGui.view.underlying.UnderlyingModel'
    ],
    title: 'Underlying Data',
    controller: 'hop-underlying',
    viewModel: {
        type: 'hop-underlying'
    },
    reference: 'underlyingGrid',
    bind: '{underlyings}',
    listeners: {
        'cellclick': 'setupChain'
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