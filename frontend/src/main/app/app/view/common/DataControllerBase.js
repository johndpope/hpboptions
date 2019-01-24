/**
 * Created by robertk on 12/13/2018.
 */
Ext.define('HopGui.view.common.DataControllerBase', {
    extend: 'Ext.app.ViewController',

    requires: [
        'Ext.Ajax',
        'HopGui.common.Definitions'
    ],

    priceRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatPrice(val);
    },

    sizeRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatSize(val);
    },

    volumeRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatVolume(val);
    },

    changeRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatChange(val);
    },

    changePctRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatChangePct(val);
    },

    ivRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatIv(val);
    },

    ivChangePctRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatIvChangePct(val);
    },

    ivRankRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatIvRank(val);
    },

    positionRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatPosition(val);
    },

    pnlRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatPnl(val);
    },

    decimalRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatDecimal(val);
    },

    decimalOneRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatDecimalOne(val);
    },

    decimalPctRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatDecimalPct(val);
    },

    wholeRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatWhole(val);
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

    formatChange: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00') : '&nbsp;';
    },

    formatChangePct: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00%') : '&nbsp;';
    },

    formatIv: function(val) {
        return (val !== 'NaN' && val > 0) ? Ext.util.Format.number(val * 100, '0.0%') : '&nbsp;';
    },

    formatIvChangePct: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.0%') : '&nbsp;';
    },

    formatIvRank: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.0') : '&nbsp;';
    },

    formatPosition: function(val) {
        return val;
    },

    formatPnl: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0') : '&nbsp;';
    },

    formatDecimal: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00') : '&nbsp;';
    },

    formatDecimalOne: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.0') : '&nbsp;';
    },

    formatDecimalPct: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.0%') : '&nbsp;';
    },

    formatWhole: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0') : '&nbsp;';
    },

    updateData: function(msg) {
        var me = this,
            arr = msg.split(','),
            id = arr[0],
            field = arr[1],
            oldVal = arr[2],
            val = arr[3];

        var selector = 'td.' + id + '.' + field;
        var td = Ext.query(selector)[0];
        if (td) {
            td.classList.remove('hop-uptick');
            td.classList.remove('hop-downtick');
            td.classList.remove('hop-unchanged');
            td.classList.remove('hop-positive');
            td.classList.remove('hop-negative');
            td.classList.remove('hop-positive-alt');
            td.classList.remove('hop-negative-alt');

            var div = Ext.query('div', true, td)[0];
            if (div) {
                if (me.isPrice(td) || me.isSize(td) || me.isIv(td) || me.isIvRank(td)) {
                    if (oldVal === 'NaN' || oldVal < 0) {
                        td.classList.add('hop-unchanged');
                    } else {
                        td.classList.add(val > oldVal ? 'hop-uptick' : (val < oldVal ? 'hop-downtick' : 'hop-unchanged'));
                    }
                } else if (me.isVolume(td)) {
                    td.classList.add('hop-unchanged');
                } else if (me.isPosition(td)|| me.isDecimal(td) || me.isDecimalOne(td) || me.isDecimalPct(td) || me.isWhole(td)) {
                    td.classList.add(val > 0 ? 'hop-positive-alt' : (val < 0 ? 'hop-negative-alt' : 'hop-positive-alt'));
                } else if (me.isChange(td) || me.isChangePct(td) || me.isIvChangePct(td) || me.isPnl(td)) {
                    td.classList.add(val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged'));
                }

                if (me.isPrice(td)) {
                    div.innerHTML = me.formatPrice(val);
                } else if (me.isChange(td)) {
                    div.innerHTML = me.formatChange(val);
                } else if (me.isChangePct(td)) {
                    div.innerHTML = me.formatChangePct(val);
                } else if (me.isIv(td)) {
                    div.innerHTML = me.formatIv(val);
                } else if (me.isIvChangePct(td)) {
                    div.innerHTML = me.formatIvChangePct(val);
                } else if (me.isIvRank(td)) {
                    div.innerHTML = me.formatIvRank(val);
                } else if (me.isPosition(td)) {
                    div.innerHTML = me.formatPosition(val);
                } else if (me.isPnl(td)) {
                    div.innerHTML = me.formatPnl(val);
                } else if (me.isDecimal(td)) {
                    div.innerHTML = me.formatDecimal(val);
                } else if (me.isDecimalOne(td)) {
                    div.innerHTML = me.formatDecimalOne(val);
                } else if (me.isDecimalPct(td)) {
                    div.innerHTML = me.formatDecimalPct(val);
                } else if (me.isWhole(td)) {
                    div.innerHTML = me.formatWhole(val);
                } else if (me.isVolume(td)) {
                    div.innerHTML = me.formatVolume(val);
                } else {
                    div.innerHTML = me.formatSize(val);
                }
            }
        }
    },

    isPrice: function(td) {
        return td.classList.contains('hop-price');
    },

    isSize: function(td) {
        return td.classList.contains('hop-size');
    },

    isVolume: function(td) {
        return td.classList.contains('hop-volume');
    },

    isChange: function(td) {
        return td.classList.contains('hop-change');
    },

    isChangePct: function(td) {
        return td.classList.contains('hop-change-pct');
    },

    isIv: function(td) {
        return td.classList.contains('hop-iv');
    },

    isIvChangePct: function(td) {
        return td.classList.contains('hop-iv-change-pct');
    },

    isIvRank: function(td) {
        return td.classList.contains('hop-iv-rank');
    },

    isPosition: function(td) {
        return td.classList.contains('hop-position');
    },

    isPnl: function(td) {
        return td.classList.contains('hop-pnl');
    },

    isDecimal: function(td) {
        return td.classList.contains('hop-decimal');
    },

    isDecimalOne: function(td) {
        return td.classList.contains('hop-decimal-one');
    },

    isDecimalPct: function(td) {
        return td.classList.contains('hop-decimal-pct');
    },

    isWhole: function(td) {
        return td.classList.contains('hop-whole');
    }
});