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
            accountDataHolders = me.getStore('accountDataHolders'),
            underlyingDataHolders = me.getStore('underlyingDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        me.refreshIbConnectionInfo();

        if (accountDataHolders) {
            accountDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/account/data-holders');
            me.refreshAccountData();
        }

        if (underlyingDataHolders) {
            underlyingDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlying/data-holders');
            me.loadUnderlyingDataHolders();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('WS underlying connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/ib_connection', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    underlyingDataHolders.reload();
                    me.refreshIbConnectionInfo();
                }
            });

            stompClient.subscribe('/topic/account', function(message) {
                me.accountDataMessageReceived(message.body);
            });

            stompClient.subscribe('/topic/underlying', function(message) {
                me.updateData(message.body);
            });

        }, function() {
            console.log('WS underlying disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
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

    refreshIbConnectionInfo: function() {
        var me = this,
            infoField = me.lookupReference('ibConnectionInfo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/connection/info',

            success: function(response) {
                var ibConnectionInfo = Ext.decode(response.responseText);
                var text = 'IB ' + ibConnectionInfo.host + ':' + ibConnectionInfo.port + ':' + ibConnectionInfo.clientId;

                infoField.update(text);
                infoField.removeCls('hop-connected');
                infoField.removeCls('hop-disconnected');
                infoField.addCls(ibConnectionInfo.connected === true ? 'hop-connected' : 'hop-disconnected');
            }
        });
    },

    refreshAccountData: function() {
        var me = this,
            accountDataHolders = me.getStore('accountDataHolders');

        accountDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded accountDataHolders');
                me.updateAccountData();
            }
        });
    },

    accountDataMessageReceived: function(msg) {
        var me = this;

        var arr = msg.split(','),
            id = arr[0],
            ibAccount = id.split('-')[1],
            field = arr[1],
            oldVal = arr[2],
            val = arr[3];

        if (!me.getAccountDataHolder(ibAccount)) {
            me.refreshAccountData();
        }

        var accountDataHolder = me.getAccountDataHolder(ibAccount);
        if (accountDataHolder) {
            accountDataHolder.set(field, val);
            me.updateAccountData();
        }
    },

    getAccountDataHolder: function(ibAccount) {
        var me = this,
            accountDataHolders = me.getStore('accountDataHolders');

        return accountDataHolders.findRecord('ibAccount', ibAccount);
    },

    updateAccountData: function() {
        var me = this,
            accountDataHolders = me.getStore('accountDataHolders'),
            accountDataField = me.lookupReference('accountData');

        var text = '';

        accountDataHolders.each(function(record) {
            var adh = record.data;
            var netLiq = adh.netLiquidationValue ? 'NetLiq ' + me.formatWhole(adh.netLiquidationValue) : '';
            var availFunds = adh.availableFunds ? 'AvailFunds ' + me.formatWhole(adh.availableFunds) : '';
            var unrlzPnl = adh.unrealizedPnl ? 'UnrlzPnl ' + me.formatWhole(adh.unrealizedPnl) : '';

            text = text + adh.ibAccount + ' ' + adh.baseCurrency + ': ' + netLiq + ', ' + availFunds + ', ' + unrlzPnl + ' ';
        });

        if (accountDataHolders.getCount() > 0) {
            accountDataField.update(text);
        }
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

    createOrder: function(view, cell, cellIndex, record, row, rowIndex, e) {
        var dataIndex = e.position.column.dataIndex;

        if (dataIndex !== 'bid' && dataIndex !== 'ask') {
            return;
        }
        var action = dataIndex === 'ask' ? 'BUY' : 'SELL';

        console.log('requesting underlying order creation ' + action + ' ' + record.data.symbol);
        Ext.Ajax.request({
            method: 'POST',
            url: HopGui.common.Definitions.urlPrefix + '/order/create-from/underlying',
            jsonData: {
                conid: record.data.conid,
                action: action
            },
            success: function(response, opts) {
                //
            }
        });
    },

    toggleDeltaHedge: function(comp, rowIndex, checked, eventColumn) {
        var me = this,
            underlyingDataHolders = me.getStore('underlyingDataHolders'),
            record = underlyingDataHolders.getAt(rowIndex),
            deltaHedge = record.data.deltaHedge;

        console.log('toggling delta hedge for underlying ' + record.data.symbol + ', deltaHedge=' + deltaHedge);
        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/underlying/' + record.data.conid + '/delta-hedge/' + deltaHedge,
            success: function(response, opts) {
                underlyingDataHolders.reload();
            }
        });
    }
});
