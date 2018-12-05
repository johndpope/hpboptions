/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.UnderlyingDataHolder'
    ],

    alias: 'viewmodel.hop-underlying',

    stores: {
        underlyingDataHolders: {
            model: 'HopGui.model.UnderlyingDataHolder',
            autoload: true
        }
    }
});