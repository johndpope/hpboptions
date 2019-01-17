/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.common.Glyphs', {
    singleton: true,

    config: {
        webFont: 'FontAwesome',
        refresh: 'xf021',
        download: 'xf019',
        play: 'xf04b',
        times: 'xf00d',
        list: 'xf03a',
        chain: 'xf0c1',
        send: 'xf1d8'
    },

    constructor: function(config) {
        this.initConfig(config);
    },

    getGlyph: function(glyph) {
        var me = this,
            font = me.getWebFont();
        if (typeof me.config[glyph] === 'undefined') {
            return false;
        }
        return me.config[glyph] + '@' + font;
    }
});