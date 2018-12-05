/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.position.PositionModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.PositionDataHolder'
    ],

    alias: 'viewmodel.hop-position',

    stores: {
        positionDataHolders: {
            model: 'HopGui.model.PositionDataHolder',
            autoload: true
        }
    }
});