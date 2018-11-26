/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.Application', {
    extend: 'Ext.app.Application',

    requires: [
        'HopGui.view.main.Main'
    ],

    name: 'HPB Options',

    stores: [
    ],
    
    launch: function () {
        var link = document.createElement('link');
        link.type = 'image/ico';
        link.rel = 'icon';
        link.href = 'resources/images/favicon.ico';
        document.getElementsByTagName('head')[0].appendChild(link);

        var main = Ext.create('HopGui.view.main.Main');

        var viewport = Ext.create('Ext.container.Viewport', {
            layout: 'fit'
        });
        viewport.add(main);
    }
});
