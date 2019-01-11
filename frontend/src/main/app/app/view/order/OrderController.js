/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.order.OrderController', {
    extend: 'HopGui.view.common.DataControllerBase',

    alias: 'controller.hop-order',

    requires: [
        'Ext.Ajax',
        'HopGui.common.Definitions'
    ],

    init: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        if (orderDataHolders) {
            orderDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/order/data-holders');
            me.loadOrderDataHolders();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('WS order connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/order', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    orderDataHolders.reload();
                } else {
                    me.updateData(message.body);
                }
            });

        }, function() {
            console.log('WS order disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    loadOrderDataHolders: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders');

        orderDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded orderDataHolders');
            }
        });
    },

    submitOrder: function (button) {
        // TODO
    },

    modifyOrder: function (button) {
        // TODO
    },

    modifyOrderIncreaseLimit: function (button) {
        // TODO
    },

    modifyOrderDecreaseLimit: function (button) {
        // TODO
    },

    cancelOrder: function (button) {
        // TODO
    },

    discardOrder: function (button) {
        // TODO
    },

    removeCompletedOrders: function(button, evt) {

    }
});