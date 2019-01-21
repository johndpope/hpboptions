/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.common.Definitions', {
    statics: {
        urlPrefix: 'http://' + window.location.host,

        getIbStatusColor: function(status) {
            var statusColor;

            switch(status) {
                case 'Submitted':   statusColor = 'blue';   break;
                case 'Cancelled':   statusColor = 'brown';  break;
                case 'Filled':      statusColor = 'green';  break;
                default:            statusColor = 'gray';   break;
            }
            return statusColor;
        }
    }
});