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
            activeChainItems.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/chain/active-items');
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
            underlyingCombo =  me.lookupReference('underlyingCombo'),
            comboStore = underlyingCombo.getStore();

        comboStore.removeAll();
        me.lookupReference('expirationCombo').getStore().removeAll();

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/chain/underlying-infos',

            success: function (response, opts) {
                var infos = Ext.decode(response.responseText).items;
                var comboData = [];

                for (var i = 0; i < infos.length; i++) {
                    comboData.push([infos[i].symbol, infos[i].conid]);
                }
                comboStore.loadData(comboData);

                Ext.Ajax.request({
                    method: 'GET',
                    url: HopGui.common.Definitions.urlPrefix + '/chain/active-key',

                    success: function(response, opts) {
                        if (response.responseText === 'NA') {
                            var comboStore = underlyingCombo.getStore();
                            underlyingCombo.select(comboStore.getAt(0));
                        } else {
                            var activeChainKey = Ext.decode(response.responseText);
                            underlyingCombo.select(activeChainKey.underlyingConid);
                        }
                    }
                });
            }
        });
    },

    prepareExpirationCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo'),
            expirationCombo =  me.lookupReference('expirationCombo'),
            comboStore = expirationCombo.getStore();

        comboStore.removeAll();

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/chain/' + underlyingCombo.getValue() + '/expirations',

            success: function (response, opts) {
                var result = Ext.decode(response.responseText);

                if (result.total === 0) {
                    console.log('expirations not ready');
                    return;
                }

                var expirations = result.items;
                var comboData = [];

                for (var i = 0; i < expirations.length; i++) {
                    var date = expirations[i]; // yyyy-MM-dd
                    var formattedDate = me.formatDate(date); // MM/dd/yyyy

                    comboData.push([formattedDate, date]);
                }
                comboStore.loadData(comboData);

                Ext.Ajax.request({
                    method: 'GET',
                    url: HopGui.common.Definitions.urlPrefix + '/chain/active-key',

                    success: function (response, opts) {
                        if (response.responseText === 'NA') {
                            expirationCombo.select(comboStore.getAt(0));

                        } else {
                            var activeChainKey = Ext.decode(response.responseText);
                            var match = false;

                            for (var i = 0; i < expirations.length; i++) {
                                if (expirations[i] === activeChainKey.expiration) {
                                    match = true;
                                    break;
                                }
                            }
                            if (match) {
                                expirationCombo.select(activeChainKey.expiration);
                                me.activateChain(); // onChange not triggered, need to call explicitly
                            } else {
                                expirationCombo.select(comboStore.getAt(0));
                            }
                        }
                    }
                });
            }
        });
    },

    activateChain: function() {
        var me = this,
            activeChainItems = me.getStore('activeChainItems'),
            underlyingConid =  me.lookupReference('underlyingCombo').getValue(),
            underlyingSymbol = me.lookupReference('underlyingCombo').getRawValue(),
            expiration =  me.lookupReference('expirationCombo').getValue(),
            chainStatus = me.lookupReference('chainStatus');

        if (!expiration) {
            console.log('expirations not ready');
            return;
        }
        activeChainItems.removeAll();

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/chain/' + underlyingConid + '/activate/' + expiration,

            success: function(response, opts) {
                if (response.responseText === 'true') {
                    chainStatus.removeCls('hop-failure');
                    chainStatus.addCls('hop-success');
                    chainStatus.update('Chain activated ' + underlyingSymbol +' ' + me.formatDate(expiration));

                    activeChainItems.load(function (records, operation, success) {
                        if (success) {
                            console.log('loaded activeChainItems');
                        }
                    });
                } else {
                    chainStatus.removeCls('hop-success');
                    chainStatus.addCls('hop-failure');
                    chainStatus.update('Chain not ready');
            }
            }
        });
    },

    createOrder: function (view, cell, cellIndex, record, row, rowIndex, e) {
        var dataIndex = e.position.column.dataIndex;
        if (dataIndex !== 'callBid' && dataIndex !== 'callAsk' && dataIndex !== 'putBid' && dataIndex !== 'putAsk') {
            return;
        }

        var isAsk = dataIndex === 'callAsk' || dataIndex === 'putAsk';
        var isCall = dataIndex === 'callBid' || dataIndex === 'callAsk';

        var callConid = record.data.call.instrument.conid;
        var callSymbol = record.data.call.instrument.symbol;

        var putConid = record.data.put.instrument.conid;
        var putSymbol = record.data.put.instrument.symbol;

        var action = isAsk ? 'BUY' : 'SELL';
        var conid = isCall ? callConid : putConid;
        var symbol = isCall ? callSymbol : putSymbol;

        console.log('requesting chain order creation ' + action + ' ' + symbol);
        Ext.Ajax.request({
            method: 'POST',
            url: HopGui.common.Definitions.urlPrefix + '/order/create-from/chain',
            jsonData: {
                conid: conid,
                action: action
            },
            success: function(response, opts) {
                //
            }
        });
    },

    formatDate: function(date) { // yyyy-MM-dd
        var dateArr = date.split('-');
        return dateArr[1] +'/' + dateArr[2] + '/' + dateArr[0]; // MM/dd/yyyy
    },
});
