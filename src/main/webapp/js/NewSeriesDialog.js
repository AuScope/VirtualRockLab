/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.NewSeriesDialog');

VRL.NewSeriesDialog = {

show: function(attributes) {

    var doClone = Ext.isObject(attributes);

    var seriesDetailsForm = new Ext.FormPanel({
        bodyStyle: 'padding:10px;',
        frame: true,
        defaults: { anchor: "100%" },
        monitorValid: true,
        items: [{
            xtype: 'label',
            text: 'Please enter the details for the new series. The name should be meaningful with at least 3 characters and the description may contain some HTML markup.'
        }, {
            xtype: 'spacer',
            height: 10
        }, {
            xtype: 'textfield',
            name: 'name',
            fieldLabel: 'Name',
            minLength: 3,
            allowBlank: false,
            maskRe: /[^\W]/,
            emptyText: 'Enter a meaningful name...'
        }, {
            xtype: 'textarea',
            name: 'description',
            fieldLabel: 'Description',
            anchor: '100% -60',
            emptyText: 'Enter a series description...'
        }]
    })

    var createSeriesHandler = function() {
        var form = seriesDetailsForm.getForm();
        if (!form.isValid()) {
            Ext.Msg.alert('Invalid values', 'Please fill in all fields of the form.');
            return false;
        }

        VRL.doRequest(VRL.controllerURL, 'createSeries',
            form.getValues(),
            function(response, options) {
                var resp = VRL.decodeResponse(response);
                if (resp) {
                    VRL.openSeries(resp.seriesId, resp.revision);
                    Ext.getCmp('nsd-win').close();
                }
            }
        );
    }

    var cloneSeriesHandler = function() {
        var form = seriesDetailsForm.getForm();
        if (!form.isValid()) {
            Ext.Msg.alert('Invalid values', 'Please fill in all fields of the form.');
            return false;
        }

        VRL.doRequest(VRL.controllerURL, 'cloneSeries',
            form.getValues(),
            function(response, options) {
                var resp = VRL.decodeResponse(response);
                if (resp) {
                    VRL.openSeries(resp.seriesId, resp.revision);
                    Ext.getCmp('nsd-win').close();
                }
            }
        );
    }

    var okHandler = (doClone ? cloneSeriesHandler : createSeriesHandler);
    var title = (doClone ? 'Copy Series' : 'Create Series');
    var w = new Ext.Window({
        id: 'nsd-win',
        title: title,
        modal: true,
        layout: 'fit',
        closable: false,
        resizable: false,
        width: 450,
        height: 300,
        initHidden: false,
        buttons: [{
            text: 'Cancel',
            handler: function() {
                w.close();
                if (!doClone) {
                    VRL.showWelcome();
                }
            }
        }, {
            text: 'OK',
            handler: okHandler
        }],
        items: seriesDetailsForm
    });

    if (doClone) {
        seriesDetailsForm.getForm().setValues(attributes);
    }
}

}

