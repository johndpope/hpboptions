/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.main.MainController', {
    extend: 'Ext.app.ViewController',

    requires: [
        'Ext.window.MessageBox'
    ],

    alias: 'controller.main',

    setGlyphs: function() {
        var me = this;

        me.lookupReference('positionPanel').setGlyph(HopGui.common.Glyphs.getGlyph('list'));
        me.lookupReference('chainPanel').setGlyph(HopGui.common.Glyphs.getGlyph('chain'));
    }
});
