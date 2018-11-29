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
            wsStatusField = me.lookupReference('wsStatus');

        me.updateIbConnectionInfo();

        if (underlyings) {
            underlyings.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlyings');
            me.loadUnderlyings();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);

        stompClient.connect({}, function(frame) {
            console.log("WS underlying connected");
            wsStatusField.update("WS connected");
            wsStatusField.addCls('hop-connected');

            stompClient.subscribe('/topic/underlying', function(message) {
                me.updateRtData(message.body);
            });

            stompClient.subscribe('/topic/ib_connection', function(message) {
                underlyings.reload();
                me.updateIbConnectionInfo();
            });

        }, function() {
            console.log("WS underlying disconnected");
            wsStatusField.update("WS disconnected");
            wsStatusField.removeCls('hop-connected');
            wsStatusField.addCls('hop-disconnected');
        });
    },

    loadUnderlyings: function() {
        var me = this,
            underlyings = me.getStore('underlyings'),
            underlyingGrid = me.lookupReference('underlyingGrid');

        underlyings.load(function(records, operation, success) {
            if (success) {
                console.log('loaded underlyings');
                underlyingGrid.setSelection(underlyings.first());
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

                infoField.update('IB ' + info);
                infoField.removeCls('hop-connected');
                infoField.removeCls('hop-disconnected');
                infoField.addCls(connected === 'true' ? 'hop-connected' : 'hop-disconnected');
            }
        });
    },

    priceRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = ' hop-underlying-' + record.data.ibRequestId;
        return me.formatPrice(val);
    },

    sizeRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId;
        return me.formatSize(val);
    },

    pctRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId + ' ' + statusCls;

        return me.formatPct(val);
    },

    formatPrice: function(val) {
        return val === 'NaN' ? '' : Ext.util.Format.number(val, '0.00###');
    },

    formatSize: function(val) {
        return val === -1 ? '' : val;
    },

    formatPct: function(val) {
        return val === 'NaN' ? '' : Ext.util.Format.number(val, '0.00%');
    },

    updateRtData: function(msg) {
        var me = this,
            arr = msg.split(","),
            ibRequestId = arr[1],
            field = arr[2],
            val = arr[3];

        var selector = 'td.hop-underlying-' + ibRequestId + '.' + field;
        var td = Ext.query(selector)[0];
        if (td) {
            td.classList.remove('hop-uptick');
            td.classList.remove('hop-downtick');
            td.classList.remove('hop-unchanged');
            td.classList.remove('hop-positive');
            td.classList.remove('hop-negative');

            var div = Ext.query('div', true, td)[0];
            if (div) {
                var oldValue = div.innerHTML;
                var newValue;

                if (td.classList.contains('hop-price')) {
                    newValue = me.formatPrice(val);
                    if (newValue > oldValue) {
                        td.classList.add('hop-uptick');
                    } else if (newValue < oldValue) {
                        td.classList.add('hop-downtick');
                    } else {
                        td.classList.add('hop-unchanged');
                    }
                } else if (td.classList.contains('hop-pct')) {
                    newValue = me.formatPct(val);
                    if (val > 0) {
                        td.classList.add('hop-positive');
                    } else if (val < 0) {
                        td.classList.add('hop-negative');
                    } else {
                        td.classList.add('hop-unchanged');
                    }
                } else {
                    newValue = me.formatSize(val);
                    if (newValue > oldValue) {
                        td.classList.add('hop-uptick');
                    } else if (newValue < oldValue) {
                        td.classList.add('hop-downtick');
                    } else {
                        td.classList.add('hop-unchanged');
                    }
                }
                div.innerHTML = newValue;
            }
        }
    },

    setupChain: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});