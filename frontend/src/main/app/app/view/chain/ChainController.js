/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.chain.ChainController', {
    extend: 'HopGui.view.common.DataControllerBase',

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
            activeChainItems.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/chain/active/items');
        }
        me.prepareUnderlyingCombo();

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log('WS chain connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/chain', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    me.prepareExpirationCombo();
                    activeChainItems.removeAll();
                } else {
                    me.updateData(message.body);
                }
            });

        }, function() {
            console.log('WS chain disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    prepareUnderlyingCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/chain/underlying-infos',

            success: function(response, opts) {
                var infos = Ext.decode(response.responseText).items;
                var comboData = [];

                for (var i = 0; i < infos.length; i++) {
                    comboData.push([infos[i].symbol, infos[i].conid]);
                }
                var comboStore = underlyingCombo.getStore();
                comboStore.loadData(comboData);

                Ext.Ajax.request({
                    method: 'GET',
                    url: HopGui.common.Definitions.urlPrefix + '/chain/active/key',

                    success: function(response, opts) {
                        if (response.responseText === 'NA') {
                            underlyingCombo.select(comboStore.getAt(0));
                        } else {
                            var activeUnderlyingConid = Ext.decode(response.responseText).underlyingConid;
                            underlyingCombo.select(activeUnderlyingConid);
                        }
                    }
                });
            }
        });
    },

    prepareExpirationCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo'),
            expirationCombo =  me.lookupReference('expirationCombo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/chain/' + underlyingCombo.getValue() + '/expirations',

            success: function(response, opts) {
                var expirations = Ext.decode(response.responseText).items;
                var comboData = [];

                for (var i = 0; i < expirations.length; i++) {
                    var date = expirations[i]; // yyyy-MM-dd
                    var formattedDate = me.formatDate(date); // MM/dd/yyyy

                    comboData.push([formattedDate, date]);
                }
                var comboStore = expirationCombo.getStore();
                comboStore.loadData(comboData);

                Ext.Ajax.request({
                    method: 'GET',
                    url: HopGui.common.Definitions.urlPrefix + '/chain/active/key',

                    success: function(response, opts) {
                        if (response.responseText === 'NA') {
                            expirationCombo.select(comboStore.getAt(0));
                        } else {
                            var activeExpiration = Ext.decode(response.responseText).expiration;
                            var match = false;

                            for (var i = 0; i < expirations.length; i++) {
                                if (expirations[i] === activeExpiration) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match) {
                                expirationCombo.select(activeExpiration);
                            } else {
                                expirationCombo.select(comboStore.getAt(0));
                            }
                        }
                    }
                });
            }
        });
    },

    formatDate: function(date) { // yyyy-MM-dd
        var dateArr = date.split('-');
        return dateArr[1] +'/' + dateArr[2] + '/' + dateArr[0]; // MM/dd/yyyy
    },

    loadChain: function() {
        var me = this,
            underlyingConid =  me.lookupReference('underlyingCombo').getValue(),
            expiration =  me.lookupReference('expirationCombo').getValue(),
            activeChainItems = me.getStore('activeChainItems'),
            chainStatus = me.lookupReference('chainStatus');

        if (!expiration) {
            console.log('expirations not ready');
            return;
        }
        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/chain/' + underlyingConid + '/activate/' + expiration,

            success: function(response, opts) {
                var chainActivationResult = Ext.decode(response.responseText)

                if (chainActivationResult.success) {
                    chainStatus.removeCls('hop-failure');
                    chainStatus.addCls('hop-success');

                    activeChainItems.load(function (records, operation, success) {
                        if (success) {
                            console.log('loaded activeChainItems');
                            chainStatus.update('Chain loaded ' + chainActivationResult.underlyingSymbol +' ' + me.formatDate(chainActivationResult.expiration));
                        }
                    });
                } else {
                    chainStatus.update('Chain not ready');
                    chainStatus.removeCls('hop-success');
                    chainStatus.addCls('hop-failure');

                    activeChainItems.removeAll();
                }
            }
        });
    },

    placeOrder: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});