/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.view.underlying.UnderlyingModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HopGui.model.Underlying'
    ],

    alias: 'viewmodel.hop-underlying',

    stores: {
        underlyings: {
            model: 'HopGui.model.Underlying',
            autoload: true
        }
    }
});