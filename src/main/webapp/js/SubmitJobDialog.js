/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.SubmitJobDialog');

VRL.SubmitJobDialog = {

// Store for ESyS-Particle versions available on the Grid
versionsStore: new Ext.data.JsonStore({
    url: VRL.JobManager.controllerURL,
    baseParams: { 'action': 'listVersions' },
    root: 'versions',
    fields: [ { name: 'value', type: 'string' } ]
}),

// Store for sites with ESyS-Particle installations (with specific version)
sitesStore: new Ext.data.JsonStore({
    url: VRL.JobManager.controllerURL,
    baseParams: { 'action': 'listSites' },
    root: 'sites',
    fields: [ { name: 'value', type: 'string' } ]
}),

// Store for queue names at a specific site
queuesStore: new Ext.data.JsonStore({
    url: VRL.JobManager.controllerURL,
    baseParams: { 'action': 'listSiteQueues' },
    root: 'queues',
    fields: [ { name: 'value', type: 'string' } ]
}),

show: function(job, callback) {
    var jobSubmitForm = new Ext.FormPanel({
        bodyStyle: 'padding:10px;',
        id: 'submit-form',
        frame: true,
        defaults: { anchor: '100%' },
        labelWidth: 140,
        monitorValid: true,
        items: [{
            xtype: 'combo',
            id: 'versions-combo',
            name: 'version',
            editable: false,
            allowBlank: false,
            store: this.versionsStore,
            triggerAction: 'all',
            displayField: 'value',
            fieldLabel: 'ESyS-Particle Version',
            emptyText: 'Select a version...'
        }, {
            xtype: 'combo',
            id: 'sites-combo',
            name: 'site',
            disabled: true,
            editable: false,
            allowBlank: false,
            store: this.sitesStore,
            triggerAction: 'all',
            displayField: 'value',
            fieldLabel: 'Site',
            emptyText: 'Select a site...'
        }, {
            xtype: 'combo',
            id: 'queues-combo',
            name: 'queue',
            disabled: true,
            editable: false,
            allowBlank: false,
            store: this.queuesStore,
            triggerAction: 'all',
            displayField: 'value',
            fieldLabel: 'Queue on Site',
            emptyText: 'Select a job queue...'
        }, {
            xtype: 'numberfield',
            name: 'walltime',
            fieldLabel: 'Max Walltime (mins)',
            allowBlank: false,
            allowDecimals: false,
            allowNegative: false,
            minValue: 1,
            value: job.walltime
        }, {
            xtype: 'numberfield',
            name: 'memory',
            fieldLabel: 'Max Memory (MB)',
            allowBlank: false,
            allowDecimals: false,
            allowNegative: false,
            minValue: 1,
            value: job.memory
        }, {
            xtype: 'numberfield',
            name: 'numprocs',
            fieldLabel: 'Number of MPI procs',
            allowBlank: false,
            allowDecimals: false,
            allowNegative: false,
            minValue: 1,
            value: job.numProcs
        }, {
            xtype: 'hidden',
            name: 'job',
            value: job.id
        }]
    });

    this.versionsStore.on({'exception': VRL.onLoadException});
    this.sitesStore.on({'exception': VRL.onLoadException});
    this.queuesStore.on({'exception': VRL.onLoadException});
    
    Ext.getCmp('versions-combo').on('select',
        function(combo, record, index) {
            var sitesCombo = Ext.getCmp('sites-combo');
            this.sitesStore.baseParams.version = record.get('value');
            this.sitesStore.reload();
            sitesCombo.enable();
            sitesCombo.reset();

            var queuesCombo = Ext.getCmp('queues-combo');
            this.queuesStore.baseParams.version = record.get('value');
            queuesCombo.disable();
            queuesCombo.reset();
        },
        this
    );

    Ext.getCmp('sites-combo').on('select',
        function(combo, record, index) {
            var queuesCombo = Ext.getCmp('queues-combo');
            this.queuesStore.baseParams.site = record.get('value');
            this.queuesStore.reload();
            queuesCombo.enable();
            queuesCombo.reset();
        },
        this
    );

    // prefill form if possible
    if (!Ext.isEmpty(job.version)) {
        var versionsCombo = Ext.getCmp('versions-combo');
        var sitesCombo = Ext.getCmp('sites-combo');
        var queuesCombo = Ext.getCmp('queues-combo');

        var onLoadQueues = function(r, opts, success) {
            if (this.queuesStore.find('value', job.queue) >= 0) {
                queuesCombo.setValue(job.queue);
            }
        }

        var onLoadSites = function(r, opts, success) {
            if (this.sitesStore.find('value', job.site) >= 0) {
                sitesCombo.setValue(job.site);
                this.queuesStore.baseParams.site = job.site;
                queuesCombo.enable();
                if (!Ext.isEmpty(job.queue)) {
                    var options = {
                        callback: onLoadQueues,
                        scope: this
                    };
                    this.queuesStore.reload(options);
                } else {
                    this.queuesStore.reload();
                }
            }
        }

        var onLoadVersions = function(r, opts, success) {
            if (this.versionsStore.find('value', job.version) >= 0) {
                versionsCombo.setValue(job.version);
                sitesCombo.enable();
                this.sitesStore.baseParams.version = job.version;
                this.queuesStore.baseParams.version = job.version;
                if (!Ext.isEmpty(job.site)) {
                    var options = {
                        callback: onLoadSites,
                        scope: this
                    };
                    this.sitesStore.reload(options);
                } else {
                    this.sitesStore.reload();
                }
            }
        }
        var options = {
            callback: onLoadVersions,
            scope: this
        };
        this.versionsStore.reload(options);
    }

    var submitBtnHandler = function() {
        var me = this;
        var onSubmitJobSuccess = function(response, request) {
            VRL.hideProgressDlg();
            Ext.getCmp('submit-win').show();
            if (VRL.decodeResponse(response)) {
                if (Ext.isFunction(callback)) {
                    Ext.getCmp('submit-win').on({'close': callback});
                }
                Ext.getCmp('submit-win').close();
            }
        }
        var onSubmitJobFailure = function(response, options) {
            VRL.hideProgressDlg();
            Ext.getCmp('submit-win').show();
            VRL.showMessage('Could not submit job! Status: '
                + response.status + ' (' + response.statusText + ')');
        }

        if (!jobSubmitForm.getForm().isValid()) {
            VRL.showMessage('Please correct the marked form fields.', 'w');
            return false;
        }

        // send details to server to submit the job
        var values = jobSubmitForm.getForm().getFieldValues();
        Ext.getCmp('submit-win').hide();
        VRL.doRequest(VRL.JobManager.controllerURL,
                'submitJob', values, onSubmitJobSuccess, onSubmitJobFailure);
    };

    var w = new Ext.Window({
        id: 'submit-win',
        title: 'Submit '+job.name,
        iconCls: 'go-icon',
        modal: true,
        layout: 'fit',
        closable: false,
        resizable: false,
        width: 400,
        height: 260,
        buttons: [{
            text: 'Cancel',
            handler: function() {
                Ext.getCmp('submit-win').close();
            }
        }, {
            text: 'Submit',
            handler: submitBtnHandler,
            scope: this
        }],
        items: [ jobSubmitForm ]
    });

    w.show();
}

}

