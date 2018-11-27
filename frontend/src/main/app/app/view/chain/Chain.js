/**
 * Created by robertk on 11/27/2018.
 */
Ext.define('HopGui.view.chain.Chain', {
    extend: 'Ext.panel.Panel',

    requires: [
        'Ext.layout.container.VBox',
        'HopGui.view.chain.ChainController',
        'HopGui.view.chain.ChainModel',
        'HopGui.view.chain.ChainGrid'
    ],
    title: 'Chains',
    xtype: 'hop-chain',
    header: false,
    border: false,
    controller: 'hop-chain',
    viewModel: {
        type: 'hop-chain'
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    scrollable: true,
    items: [{
        xtype: 'hop-chain-grid',
        reference: 'chainGrid'
    }]
});