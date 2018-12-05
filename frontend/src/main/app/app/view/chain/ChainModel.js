/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.ChainDataHolder'
    ],

    alias: 'viewmodel.hop-chain',

    stores: {
        chainDataHolders: {
            model: 'HopGui.model.ChainDataHolder',
            autoload: true
        }
    }
});