/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.JobDetailsDialog');

VRL.JobDetailsDialog = {

show: function(callback, values) {
    var jobDetailsForm = new Ext.FormPanel({
        bodyStyle: 'padding:10px;',
        frame: true,
        defaults: { anchor: "100%" },
        monitorValid: true,
        items: [{
            xtype: 'textfield',
            name: 'name',
            fieldLabel: 'Job Name',
            minLength: 3,
            allowBlank: false,
            maskRe: /[^\W]/,
            emptyText: 'Enter a meaningful name...'
        }, {
            xtype: 'textfield',
            name: 'scriptFile',
            fieldLabel: 'Script File',
            allowBlank: false,
            maskRe: /[\w+#@.,-]/,
            regex: /^([\w]+)([\w+#@.,-])*\.py$/,
            regexText: 'The filename must end in .py',
            emptyText: 'Enter the input script filename...'
        }, {
            xtype: 'textarea',
            name: 'description',
            fieldLabel: 'Description',
            anchor: '100% -60',
            emptyText: 'Enter a job description...'
        }, {
            xtype: 'hidden',
            name: 'id'
        }]
    });

    var newJob = true;

    if (Ext.isObject(values)) {
        jobDetailsForm.getForm().setValues(values);
        newJob = false;
    }

    var saveJobHandler = function() {
        var form = jobDetailsForm.getForm();
        if (!form.isValid()) {
            Ext.Msg.alert('Invalid values',
                          'Please correct the marked form fields.');
            return false;
        }

        var onSaveSuccess = function(response, options) {
            VRL.hideProgressDlg();
            Ext.getCmp('jdd-win').show();
            var resp = VRL.decodeResponse(response);
            if (resp) {
                if (Ext.isFunction(callback)) {
                    Ext.getCmp('jdd-win').on({
                        'close': callback.createCallback(resp.job)
                    });
                }
                Ext.getCmp('jdd-win').close();
            }
        }

        var onSaveFailure = function(response, options) {
            VRL.hideProgressDlg();
            Ext.getCmp('jdd-win').show();
            VRL.showError('Could not execute last request. Status: '+
                response.status+' ('+response.statusText+')');
        }

        Ext.getCmp('jdd-win').hide();
        VRL.doRequest(VRL.JobManager.controllerURL,
            'saveJob', form.getValues(), onSaveSuccess, onSaveFailure);
    }

    var w = new Ext.Window({
        id: 'jdd-win',
        title: 'Enter Job Details',
        modal: true,
        layout: 'fit',
        closable: false,
        width: 400,
        height: 250,
        initHidden: false,
        buttons: [{
            text: 'Cancel',
            handler: function() { w.close(); }
        }, {
            text: newJob ? 'Create' : 'Save',
            handler: saveJobHandler
        }],
        items: jobDetailsForm
    });
}

}

