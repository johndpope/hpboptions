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
            me.prepareOrderFilter();
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

    prepareOrderFilter: function() {
        var me = this,
            orderFilter = me.lookupReference('orderFilter');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/order/filter',

            success: function(response, opts) {
                var filterResult = Ext.decode(response.responseText);

                orderFilter.setValue({
                    showNew: filterResult.showNew,
                    showWorking: filterResult.showWorking,
                    showCompleted: filterResult.showCompleted
                });
            }
        });
    },

    onOrderFilterChange: function(radioGroup, newValue, oldValue, eOpts) {
        var me = this;

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/filter',
            jsonData: {
                showNew: !!newValue.showNew,
                showWorking: !!newValue.showWorking,
                showCompleted: !!newValue.showCompleted
            },
            success: function(response, opts) {
                me.loadOrderDataHolders();
            }
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
        var order = button.getWidgetRecord().data;

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/' + order.orderId + '/send',
            jsonData: {
                orderId: order.orderId,
                quantity: order.quantity,
                limitPrice: order.limitPrice,
                adapt: order.adapt
            },
            success: function(response, opts) {
                // reload triggered through ws reloadRequest
            }
        });
    },

    sendNewOrders: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders');

        var sendOrderParamsArr = [];

        orderDataHolders.each(function(record) {
            var order = record.data;

            if (order.state === 'New') {
                sendOrderParamsArr.push({
                    orderId: order.orderId,
                    quantity: order.quantity,
                    limitPrice: order.limitPrice,
                    adapt: order.adapt
                });
            }
        });

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/send',
            jsonData: sendOrderParamsArr,

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

    removeIdleOrders: function() {
        var me = this,
            orderDataHolders = me.getStore('orderDataHolders');

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/order/remove-idle',
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