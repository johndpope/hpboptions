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

        metadata.tdCls = val > 0 ? 'hop-positive-pos' : (val < 0 ? 'hop-negative-pos' : 'hop-positive-pos');
        return me.formatPosition(val);
    },

    plRenderer: function(val, metadata, record) {
        var me = this;

        var statusCls = val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged');
        metadata.tdCls = record.data.id + ' ' + statusCls;

        return me.formatPl(val);
    },

    decimalRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatDecimal(val);
    },

    decimalPctRenderer: function(val, metadata, record) {
        var me = this;

        metadata.tdCls = record.data.id;
        return me.formatDecimalPct(val);
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
        return (val > 0 && val < Number.MAX_VALUE)  ? Ext.util.Format.number(val * 100, '0.0%') : '&nbsp;';
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

    formatPl: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00') : '&nbsp;';
    },

    formatDecimal: function(val) {
        return (val !== 'NaN' && val < Number.MAX_VALUE) ? Ext.util.Format.number(val, '0.00') : '&nbsp;';
    },

    formatDecimalPct: function(val) {
        return val !== 'NaN' ? Ext.util.Format.number(val, '0.00%') : '&nbsp;';
    },

    updateData: function(msg) {
        var me = this,
            arr = msg.split(","),
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
            td.classList.remove('hop-positive-pos');
            td.classList.remove('hop-negative-pos');

            var div = Ext.query('div', true, td)[0];
            if (div) {
                if (me.isPrice(td) || me.isSize(td) || me.isIv(td) || me.isIvRank(td) || me.isDecimal(td) || me.isDecimalPct(td)) {
                    if (oldVal < 0 || oldVal === 'NaN') {
                        td.classList.add('hop-unchanged');
                    } else {
                        td.classList.add(val > oldVal ? 'hop-uptick' : (val < oldVal ? 'hop-downtick' : 'hop-unchanged'));
                    }
                } else if (me.isVolume(td)) {
                    td.classList.add('hop-unchanged');
                } else if (me.isPosition(td)) {
                    td.classList.add(val > 0 ? 'hop-positive-pos' : (val < 0 ? 'hop-negative-pos' : 'hop-positive-pos'));
                } else if (me.isChangePct(td) || me.isIvChangePct(td) || me.isPl(td)) {
                    td.classList.add(val > 0 ? 'hop-positive' : (val < 0 ? 'hop-negative' : 'hop-unchanged'));
                }

                if (me.isPrice(td)) {
                    div.innerHTML = me.formatPrice(val);
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
                } else if (me.isPl(td)) {
                    div.innerHTML = me.formatPl(val);
                } else if (me.isDecimal(td)) {
                    div.innerHTML = me.formatDecimal(val);
                } else if (me.isDecimalPct(td)) {
                    div.innerHTML = me.formatDecimalPct(val);
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

    isPl: function(td) {
        return td.classList.contains('hop-pl');
    },

    isDecimal: function(td) {
        return td.classList.contains('hop-decimal');
    },

    isDecimalPct: function(td) {
        return td.classList.contains('hop-decimal-pct');
    }
});