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

    sendOrder: function (button) {
        var me = this,
            order = button.getWidgetRecord().data;

        me.doSend(order);
    },

    sendNewOrders: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders');

        orderDataHolders.each(function(record) {
            var order = record.data;
            if (order.state === 'New') {
                me.doSend(order);
            }
        });
    },

    doSend: function(order) {
        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/' + order.orderId + '/send',
            jsonData: {
                quantity: order.quantity,
                limitPrice: order.limitPrice
            },
            success: function(response, opts) {
                // reload triggered through ws reloadRequest
            }
        });
    },

    cancelOrder: function (button) {
        var order = button.getWidgetRecord().data;

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/' + order.orderId + '/cancel',
            success: function(response) {
                // reload triggered through ws reloadRequest
            }
        });
    },

    removeNonworkingOrders: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders');

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/remove-nonworking',
            success: function(response) {
                orderDataHolders.reload();
            }
        });
    },

    orderPriceRenderer: function(val, metadata, record) {
        var me = this;
        return me.formatPrice(val);
    }
});