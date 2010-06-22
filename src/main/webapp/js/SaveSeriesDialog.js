/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.SaveSeriesDialog');

VRL.SaveSeriesDialog = {

show: function(callback) {
    var saveSeriesHandler = function() {
        var form = Ext.getCmp('ssd-form').getForm();
        if (!form.isValid()) {
            VRL.showMessage('Please enter a descriptive message.', 'w');
            return false;
        }

        VRL.doRequest(VRL.controllerURL, 'saveSeries',
            form.getValues(),
            function(response, options) {
                var resp = VRL.decodeResponse(response);
                if (resp) {
                    if (resp.revision == -1) {
                        VRL.showMessage(
                            'The series is up to date. No save required.', 'i');
                    } else {
                        if (Ext.isFunction(callback)) {
                            callback(resp);
                        }
                    }
                }
            }
        );
        Ext.getCmp('ssd-win').close();
    }

    var w = new Ext.Window({
        id: 'ssd-win',
        title: 'Save Series',
        modal: true,
        layout: 'fit',
        closable: false,
        resizable: false,
        width: 400,
        height: 250,
        initHidden: false,
        buttons: [{
            text: 'Cancel',
            handler: function() {
                w.close();
            }
        }, {
            text: 'OK',
            handler: saveSeriesHandler
        }],
        items: new Ext.FormPanel({
            id: 'ssd-form',
            bodyStyle: 'padding:10px;',
            frame: true,
            defaults: { anchor: "100%" },
            labelWidth: 60,
            monitorValid: true,
            items: [{
                xtype: 'label',
                text: 'This will store all changed files in the revision control system under a new series revision. Please briefly explain the changes you made.'
            }, {
                xtype: 'spacer',
                height: 10
            }, {
                xtype: 'textarea',
                name: 'message',
                fieldLabel: 'Commit Message',
                allowBlank: false,
                anchor: '100% -30',
                emptyText: 'Enter a description of the changes...'
            }]
        })
    });
}

}

