/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.ChainItem'
    ],

    alias: 'viewmodel.hop-chain',

    stores: {
        activeChainItems: {
            model: 'HopGui.model.ChainItem',
            autoload: true,
            pageSize: 1000 // disable
        }
    }
});