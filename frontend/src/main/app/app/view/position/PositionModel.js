/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.Position'
    ],

    alias: 'viewmodel.hop-position',

    stores: {
        positions: {
            model: 'HopGui.model.Position',
            autoload: true
        }
    }
});