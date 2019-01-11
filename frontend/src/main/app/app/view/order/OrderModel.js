/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.OrderDataHolder'
    ],

    alias: 'viewmodel.hop-order',

    stores: {
        orderDataHolders: {
            model: 'HopGui.model.OrderDataHolder',
            autoload: true,
            pageSize: 1000 // disable
        }
    }
});