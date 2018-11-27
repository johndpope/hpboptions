/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hop-underlying',

    requires: [
        'Ext.Ajax',
        'HopGui.common.Definitions'
    ],

    init: function() {
        var me = this,
            underlyings = me.getStore('underlyings'),
            underlyingGrid = me.lookupReference('underlyingGrid');

        if (underlyings) {
            underlyings.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlyings');
            underlyings.load(function(records, operation, success) {
                if (success) {
                    console.log('reloaded underlyings');
                    underlyingGrid.setSelection(underlyings.first());
                }
            });
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log("WS underlying connected");

            stompClient.subscribe('/topic/underlying', function(message) {
                // TODO
            });

        }, function() {
            console.log("WS underlying disconnected");
        });
    },

    setupChain: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});