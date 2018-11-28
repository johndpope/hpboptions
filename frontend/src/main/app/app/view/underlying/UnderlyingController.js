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
            underlyings = me.getStore('underlyings');

        if (underlyings) {
            underlyings.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlyings');
            me.reloadUnderlyings();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log("WS underlying connected");
            // TODO WS status

            stompClient.subscribe('/topic/underlying', function(message) {
                me.updateRtData(message.body);
            });

            stompClient.subscribe('/topic/ib_connection', function(message) {
                me.reloadUnderlyings();
            });

        }, function() {
            console.log("WS underlying disconnected");
            // TODO WS status
        });
    },

    reloadUnderlyings: function() {
        var me = this,
            underlyings = me.getStore('underlyings'),
            underlyingGrid = me.lookupReference('underlyingGrid');

        underlyings.load(function(records, operation, success) {
            if (success) {
                console.log('reloaded underlyings');
                underlyingGrid.setSelection(underlyings.first());
                me.updateIbConnectionInfo();
            }
        });
    },

    connect: function(button, e, options) {
        var box = Ext.MessageBox.wait('Connecting', 'Action in progress');

        Ext.Ajax.request({
            method: 'PUT',
            url: HopGui.common.Definitions.urlPrefix + '/connect',
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
            url: HopGui.common.Definitions.urlPrefix + '/disconnect',
            success: function(response) {
                box.hide();
            },
            failure: function() {
                box.hide();
            }
        });
    },

    updateIbConnectionInfo: function() {
        var me = this,
            infoField = me.lookupReference('ibConnectionInfo');

        Ext.Ajax.request({
            method: 'GET',
            url: HopGui.common.Definitions.urlPrefix + '/connection-info',
            success: function(response) {
                var arr = response.responseText.split(","),
                    info = arr[0],
                    connected = arr[1];

                infoField.update(info);
                infoField.removeCls('hop-connected');
                infoField.removeCls('hop-disconnected');
                infoField.addCls(connected === 'true' ? 'hop-connected' : 'hop-disconnected');
            }
        });
    },

    priceRenderer: function(val, metadata, record) {
        metadata.tdCls = ' hop-underlying-' + record.data.ibRequestId;
        return val === 'NaN' ? '' : Ext.util.Format.number(val, '0.00###');
    },

    sizeRenderer: function(val, metadata, record) {
        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId;
        return val === -1 ? '' : val;
    },

    pctRenderer: function(val, metadata, record) {
        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId;
        return val === 'NaN' ? '' : Ext.util.Format.number(val, '0.00%');
    },

    updateRtData: function(msg) {
        var arr = msg.split(","),
            ibRequestId = arr[1],
            field = arr[2],
            val = arr[3];

        var selector = 'td.hop-underlying-' + ibRequestId + '.' + field;
        var td = Ext.query(selector)[0];
        if (td) {
            var div = Ext.query('div', true, td)[0];
            if (div) {
                var formattedValue;
                if (td.classList.contains('hop-price')) {
                    formattedValue = Ext.util.Format.number(val, '0.00###');
                } else if (td.classList.contains('hop-pct')) {
                    formattedValue = Ext.util.Format.number(val, '0.00%');
                } else {
                    formattedValue = val;
                }
                div.innerHTML = formattedValue;
            }
        }
    },

    setupChain: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});