/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.common.Glyphs', {
    singleton: true,

    config: {
        webFont: 'FontAwesome',
        add: 'xf067',
        edit: 'xf040',
        delete: 'xf1f8',
        save: 'xf00c',
        cancel: 'xf0e2',
        refresh: 'xf021',
        barchart: 'xf080',
        orderedlist: 'xf0cb',
        money: 'xf0d6',
        gear: 'xf013',
        destroy: 'xf1f8',
        download: 'xf019',
        play: 'xf04b',
        stop: 'xf04d',
        list: 'xf03a',
        chain: 'xf0c1'
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