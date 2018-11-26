/**
 * Created by robertk on 11/26/2018.
 */
Ext.define('HopGui.common.Definitions', {
    statics: {
        urlPrefix: 'http://' + window.location.host,

        getIbOrderStatusColor: function(status) {
            var statusColor;

            switch(status) {
                case 'SUBMITTED':   statusColor = 'blue';   break;
                case 'UPDATED':     statusColor = 'blue';   break;
                case 'CANCELLED':   statusColor = 'brown';  break;
                case 'FILLED':      statusColor = 'green';  break;
                case 'UNKNOWN':     statusColor = 'gray';   break;
            }
            return statusColor;
        }
    }
});