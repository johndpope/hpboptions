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

        me.lookupReference('positionGrid').setGlyph(HopGui.common.Glyphs.getGlyph('list'));
        me.lookupReference('chainGrid').setGlyph(HopGui.common.Glyphs.getGlyph('chain'));
    }
});
