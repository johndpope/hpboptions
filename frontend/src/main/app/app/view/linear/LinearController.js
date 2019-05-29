/**
 * Created by robertk on 5/29/2019.
 */
Ext.define('HopGui.view.linear.LinearController', {
    extend: 'HopGui.view.common.DataControllerBase',

    alias: 'controller.hop-linear',

    requires: [
        'HopGui.view.common.DataControllerBase'
    ],

    init: function() {
        var me = this,
            linearDataHolders = me.getStore('linearDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        if (linearDataHolders) {
            linearDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/linear/data-holders');
            me.loadLinearDataHolders();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('WS linear connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/linear', function(message) {
                me.updateData(message.body);
            });

        }, function() {
            console.log('WS linear disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    loadLinearDataHolders: function() {
        var me = this,
            linearDataHolders = me.getStore('linearDataHolders');

        linearDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded linearDataHolders');
            }
        });
    },

    createOrder: function(view, cell, cellIndex, record, row, rowIndex, e) {
        var dataIndex = e.position.column.dataIndex;

        if (dataIndex !== 'bid' && dataIndex !== 'ask') {
            return;
        }
        var action = dataIndex === 'ask' ? 'BUY' : 'SELL';

        console.log('requesting linear order creation ' + action + ' ' + record.data.symbol);
        Ext.Ajax.request({
            method: 'POST',
            url: HopGui.common.Definitions.urlPrefix + '/order/create-from/linear',
            jsonData: {
                conid: record.data.conid,
                action: action
            },
            success: function(response, opts) {
                //
            }
        });
    }
});
