/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingController', {
    extend: 'HopGui.view.common.DataControllerBase',

    alias: 'controller.hop-underlying',

    requires: [
        'HopGui.view.common.DataControllerBase'
    ],

    init: function() {
        var me = this,
            underlyingDataHolders = me.getStore('underlyingDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        me.refreshIbConnectionInfo();
        me.refreshAccountSummary();

        if (underlyingDataHolders) {
            underlyingDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlying/data-holders');
            me.loadUnderlyingDataHolders();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log("WS underlying connected");
            wsStatusField.update("WS connected");
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/ib_connection', function(message) {
                underlyingDataHolders.reload();
                me.updateIbConnectionInfo(message.body);
            });

            stompClient.subscribe('/topic/account', function(message) {
                me.updateAccountSummary(message.body);
            });

            stompClient.subscribe('/topic/underlying', function(message) {
                me.updateData(message.body);
            });

        }, function() {
            console.log("WS underlying disconnected");

            wsStatusField.update("WS disconnected");
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    loadUnderlyingDataHolders: function() {
        var me = this,
            underlyingDataHolders = me.getStore('underlyingDataHolders');

        underlyingDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded underlyingDataHolders');
            }
        });
    },

    connect: function(button, e, options) {
        var box = Ext.MessageBox.wait('Connecting', 'Action in progress');

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/connection/connect',
            success: function(response) {
                box.hide();
            },
            failure: function() {
                box.hide();
            }
        });
    },

    disconnect: function(button, e, options) {
        var box = Ext.MessageBox.wait('Disconnecting', 'Action in progress');

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/connection/disconnect',
            success: function(response) {
                box.hide();
            },
            failure: function() {
                box.hide();
            }
        });
    },

    updateIbConnectionInfo: function(message) {
        var me = this,
            infoField = me.lookupReference('ibConnectionInfo');

        var arr = message.split(","),
            info = arr[0],
            connected = arr[1];

        infoField.update('IB ' + info);
        infoField.removeCls('hop-connected');
        infoField.removeCls('hop-disconnected');
        infoField.addCls(connected === 'true' ? 'hop-connected' : 'hop-disconnected');
    },

    refreshIbConnectionInfo: function() {
        var me = this;

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/connection/info',
            success: function(response) {
                me.updateIbConnectionInfo(response.responseText);
            }
        });
    },

    updateAccountSummary: function(message) {
        var me = this;

        me.lookupReference('accountSummary').update(message);
    },

    refreshAccountSummary: function() {
        var me = this;

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/account/summary',
            success: function(response) {
                me.updateAccountSummary(response.responseText);
            }
        });
    }
});