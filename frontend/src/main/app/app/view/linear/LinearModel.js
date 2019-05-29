/**
 * Created by robertk on 5/29/2019.
 */
Ext.define('HopGui.view.linear.LinearModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.LinearDataHolder'
    ],

    alias: 'viewmodel.hop-linear',

    stores: {
        linearDataHolders: {
            model: 'HopGui.model.LinearDataHolder',
            autoload: true,
            pageSize: 1000 // disable
        }
    }
});
