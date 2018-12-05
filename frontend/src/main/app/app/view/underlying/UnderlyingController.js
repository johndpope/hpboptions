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
            underlyingDataHolders = me.getStore('underlyingDataHolders'),
            wsStatusField = me.lookupReference('wsStatus');

        me.updateIbConnectionInfo();

        if (underlyingDataHolders) {
            underlyingDataHolders.getProxy().setUrl(HopGui.common.Definitions.urlPrefix + '/underlying-data-holders');
            me.loadUnderlyingDataHolders();
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
                underlyingDataHolders.reload();
                me.updateIbConnectionInfo();
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
            underlyingDataHolders = me.getStore('underlyingDataHolders'),
            underlyingGrid = me.lookupReference('underlyingGrid');

        underlyingDataHolders.load(function(records, operation, success) {
            if (success) {
                console.log('loaded underlyingDataHolders');
                underlyingGrid.setSelection(underlyingDataHolders.first());
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

    volumeRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId;
        return me.formatVolume(val);
    },

    changePctRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId + ' ' + statusCls;

        return me.formatChangePct(val);
    },

    ivRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = 'hop-underlying-' + record.data.ibRequestId;
        return me.formatIv(val);
    },

    formatPrice: function(val) {
        return val > 0 ? Ext.util.Format.number(val, '0.00###') : '&nbsp;';
    },

    formatSize: function(val) {
        return val > 0 ? val : '&nbsp;';
    },

    formatVolume: function(val) {
        return val > 0 ? d3.format('.3~s')(val) : '&nbsp;';
    },

    formatChangePct: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00%') : '&nbsp;';
    },

    formatIv: function(val) {
        return val > 0 ? Ext.util.Format.number(val, '0.0%') : '&nbsp;';
    },

    updateRtData: function(msg) {
        var me = this,
            arr = msg.split(","),
            ibRequestId = arr[1],
            field = arr[2],
            oldVal = arr[3],
            val = arr[4];

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
                if (me.isPrice(td) || me.isSize(td) || me.isIv(td)) {
                    if (oldVal < 0) {
                        td.classList.add('hop-unchanged');
                    } else {
                        td.classList.add(val > oldVal ? 'hop-uptick' : (val < oldVal ? 'hop-downtick' : 'hop-unchanged'));
                    }
                } else if (me.isVolume(td)) {
                    td.classList.add('hop-unchanged');
                } else if (me.isChangePct(td)) {
                    td.classList.add(val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged'));
                }

                if (me.isPrice(td)) {
                    div.innerHTML = me.formatPrice(val);
                } else if (me.isChangePct(td)) {
                    div.innerHTML = me.formatChangePct(val);
                } else if (me.isIv(td)) {
                    div.innerHTML = me.formatIv(val);
                } else {
                    div.innerHTML = me.isVolume(td) ? me.formatVolume(val) : me.formatSize(val);
                }
            }
        }
    },

    isPrice: function(td) {
        return td.classList.contains('hop-price');
    },

    isChangePct: function(td) {
        return td.classList.contains('hop-change-pct');
    },

    isIv: function(td) {
        return td.classList.contains('hop-iv');
    },

    isSize: function(td) {
        return td.classList.contains('hop-size');
    },

    isVolume: function(td) {
        return td.classList.contains('hop-volume');
    },

    setupChain: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    }
});