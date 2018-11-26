/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.hop-position',

    requires: [
        'Ext.Ajax',
        'HopGui.common.Definitions'
    ],

    init: function() {
        // TODO
    },

    placeOrder: function (view, cell, cellIndex, record, row, rowIndex, e) {
        // TODO
    },

    setGlyphs: function() {
        var me = this;

        me.lookupReference('position').setGlyph(HopGui.common.Glyphs.getGlyph('list'));
    }
});