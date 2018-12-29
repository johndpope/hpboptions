/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.model.ChainItem', {
    extend: 'Ext.data.Model',

    idProperty: 'strike',
    fields: [
        'strike',
        'call',
        {name: 'callBid', mapping: 'call.bid'},
        {name: 'callAsk', mapping: 'call.ask'},
        {name: 'callLast', mapping: 'call.last'},
        {name: 'callClose', mapping: 'call.close'},
        {name: 'callChangePct', mapping: 'call.changePct'},
        {name: 'callBidSize', mapping: 'call.bidSize'},
        {name: 'callAskSize', mapping: 'call.askSize'},
        {name: 'callLastSize', mapping: 'call.lastSize'},
        {name: 'callVolume', mapping: 'call.volume'},
        {name: 'callTimeValue', mapping: 'call.timeValue'},
        {name: 'callTimeValuePct', mapping: 'call.timeValuePct'},
        {name: 'callDelta', mapping: 'call.delta'},
        {name: 'callGamma', mapping: 'call.gamma'},
        {name: 'callImpliedVol', mapping: 'call.impliedVol'},
        {name: 'callOptionOpenInterest', mapping: 'call.optionOpenInterest'},

        'put',
        {name: 'putBid', mapping: 'put.bid'},
        {name: 'putAsk', mapping: 'put.ask'},
        {name: 'putLast', mapping: 'put.last'},
        {name: 'putClose', mapping: 'put.close'},
        {name: 'putChangePct', mapping: 'put.changePct'},
        {name: 'putBidSize', mapping: 'put.bidSize'},
        {name: 'putAskSize', mapping: 'put.askSize'},
        {name: 'putLastSize', mapping: 'put.lastSize'},
        {name: 'putVolume', mapping: 'put.volume'},
        {name: 'putTimeValue', mapping: 'put.timeValue'},
        {name: 'putTimeValuePct', mapping: 'put.timeValuePct'},
        {name: 'putDelta', mapping: 'put.delta'},
        {name: 'putGamma', mapping: 'put.gamma'},
        {name: 'putImpliedVol', mapping: 'put.impliedVol'},
        {name: 'putOptionOpenInterest', mapping: 'put.optionOpenInterest'}
    ],
    schema: {
        id: 'hopSchema',
        namespace: 'HopGui.model',  // generate auto entityName,
        proxy: {
            type: 'ajax',
            actionMethods: {
                read: 'GET',
                update: 'PUT'
            },
            reader: {
                type: 'json',
                rootProperty: 'items',
                totalProperty: 'total'
            }
        }
    }
});