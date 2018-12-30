/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hop-chain',

    requires: [
        'Ext.Ajax',
        'HopGui.common.Definitions'
    ],

    init: function() {
        var me = this,
            activeChainItems = me.getStore('activeChainItems'),
            wsStatusField = me.lookupReference('wsStatus');

        if (activeChainItems) {
            activeChainItems.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/active-chain-items');
        }
        me.prepareUnderlyingCombo();

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log("WS chain connected");
            wsStatusField.update("WS connected");
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/chain', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    me.prepareExpirationCombo();
                    activeChainItems.reload();
                } else {
                    me.updateData(message.body);
                }
            });

        }, function() {
            console.log("WS chain disconnected");

            wsStatusField.update("WS disconnected");
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    prepareUnderlyingCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/underlying-infos',

            success: function(response, opts) {
                var infos = Ext.decode(response.responseText);
                var comboData = [];

                for (var i = 0; i < infos.length; i++) {
                    comboData.push([infos[i].symbol, infos[i].conid]);
                }
                underlyingCombo.getStore().loadData(comboData);
                underlyingCombo.setValue(comboData[0].conid);
            }
        });
    },

    prepareExpirationCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo'),
            expirationCombo =  me.lookupReference('expirationCombo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/expirations/' + underlyingCombo.getValue(),

            success: function(response, opts) {
                var expirations = Ext.decode(response.responseText);
                var comboData = [];

                for (var i = 0; i < expirations.length; i++) {
                    var date = expirations[i]; // yyyy-MM-dd
                    var dateArr = date.split('-');

                    var formattedDate = dateArr[1] +'/' + dateArr[2] + '/' + dateArr[0]; // MM/dd/yyyy
                    comboData.push([formattedDate, date]);
                }
                expirationCombo.getStore().loadData(comboData);
                if (comboData.length >= 1) {
                    expirationCombo.setValue(comboData[0].date);
                }
            }
        });
    },

    activateChain: function() {
        // TODO check if expiration not null
    },

    loadActiveChainItems: function() {
        var me = this,
            activeChainItems = me.getStore('activeChainItems');

        activeChainItems.load(function(records, operation, success) {
            if (success) {
                console.log('loaded activeChainItems');
            }
        });
    },

    placeOrder: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});