/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionController', {
    extend: 'HopGui.view.common.DataControllerBase',

    alias: 'controller.hop-position',

    requires: [
        'HopGui.view.common.DataControllerBase'
    ],

    init: function() {
        var me = this,
            positionDataHolders = me.getStore('positionDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        if (positionDataHolders) {
            positionDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/position/data-holders');
            me.loadPositionDataHolders();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('WS position connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/position', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    positionDataHolders.reload();
                } else {
                    me.updateData(message.body);
                }
            });

        }, function() {
            console.log('WS position disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    loadPositionDataHolders: function() {
        var me = this,
            positionDataHolders = me.getStore('positionDataHolders');

        positionDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded positionDataHolders');
            }
        });
    },

    createOrder: function (view, cell, cellIndex, record, row, rowIndex, e) {
        var dataIndex = e.position.column.dataIndex;
        if (dataIndex !== 'bid' && dataIndex !== 'ask') {
            return;
        }
        var action = dataIndex === 'ask' ? 'BUY' : 'SELL';

        console.log('requesting position order creation ' + action + ' ' + record.data.symbol);
        Ext.Ajax.request({
            method: 'POST',
            url: HopGui.common.Definitions.urlPrefix + '/order/create/from/position',
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