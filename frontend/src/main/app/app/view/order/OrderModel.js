/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.Order'
    ],

    alias: 'viewmodel.hop-order',

    stores: {
        orders: {
            model: 'HopGui.model.Order',
            autoload: true
        }
    }
});