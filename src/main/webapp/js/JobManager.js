/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.JobManager');

VRL.JobManager = {

    // URL that handles action requests
    controllerURL: 'job.do',

    // Template for the job details area
    jobDetailsTpl: new Ext.Template(
        '<p class="seriesdesc-title">{name}</p>',
        '<table class="seriesdesc">',
        '<tr><th>Submitted on:</th><td>{submitDate:this.dateFmt}</td></tr>',
        '<tr><th>Site:</th><td>{site}</td></tr>',
        '<tr><th>ESyS-Particle Version:</th><td>{version}</td></tr>',
        '<tr><th>Status:</th><td>{status:this.statusFmt}</td></tr>',
        '<tr><th>Input Script Filename:</th><td>{scriptFile}</td></tr>',
        '<tr><th>Description:</th><td>{description}</td></tr>',
        '<tr><th>Standard Output:</th><td><pre>{stdout}</pre></td></tr>',
        '<tr><th>Standard Error:</th><td><pre>{stderr}</pre></td></tr>',
        '</table>',
        {
            dateFmt: function(value) {
                if (!Ext.isEmpty(value)) {
                    return Ext.util.Format.date(
                        new Date(value), 'd M Y, g:i:s a');
                } else {
                    return "Unsubmitted";
                }
            },
            statusFmt: function(value) {
                if (value === 'Failed') {
                    return '<span style="color:red;">' + value + '</span>';
                } else if (value === 'Active') {
                    return '<span style="color:green;">' + value + '</span>';
                } else if (value === 'Done') {
                    return '<span style="color:blue;">' + value + '</span>';
                }
                return value || '';
            },
            compiled: true
        }
    ),

    //
    // ACTIONS
    //

    deleteAction: new Ext.Action({
        text: 'Delete',
        iconCls: 'deletefile-icon',
        tooltip: 'Deletes the selected job',
        handler: function() {
            var job = VRL.JobManager.jobGrid
                .getSelectionModel().getSelected().data;
            VRL.JobManager.deleteJob(job);
        },
        disabled: true
    }),

    editAction: new Ext.Action({
        text: 'Edit',
        iconCls: 'edit-icon',
        tooltip: 'Opens the job detail editor',
        handler: function() {
            VRL.JobDetailsDialog.show(
                function(newJob) {
                    VRL.JobManager.jobGrid.getStore().reload();
                },
                VRL.JobManager.jobGrid.getSelectionModel()
                    .getSelected().data
            );
        },
        disabled: true
    }),

    killAction: new Ext.Action({
        text: 'Terminate',
        iconCls: 'killjob-icon',
        tooltip: 'Terminates the job',
        handler: function() {
            var job = VRL.JobManager.jobGrid
                .getSelectionModel().getSelected().data;
            VRL.JobManager.killJob(job);
        },
        disabled: true
    }),

    newJobAction: new Ext.Action({
        text: 'New...',
        iconCls: 'new-icon',
        tooltip: 'Creates a new job',
        handler: function() {
            VRL.JobDetailsDialog.show(
                function(newJob) {
                    VRL.JobManager.jobGrid.getStore().reload();
                }
            );
        }
    }),

    refreshAction: new Ext.Action({
        text: 'Refresh',
        iconCls: 'refresh-icon',
        tooltip: 'Reloads all job details from the server',
        handler: function() {
            VRL.JobManager.reloadJobs();
        }
    }),

    submitAction: new Ext.Action({
        text: 'Submit...',
        iconCls: 'go-icon',
        tooltip: 'Opens the job submission dialog',
        handler: function() {
            var job = VRL.JobManager.jobGrid
                .getSelectionModel().getSelected().data;
            VRL.JobManager.submitJob(job)
        },
        disabled: true
    }),

    SUBMIT_STATES: ['Done', 'Failed'],

    // issues a Delete Job request after asking for confirmation
    deleteJob: function(job) {
        if (!Ext.isEmpty(job.submitDate)
                && this.SUBMIT_STATES.indexOf(job.status) < 0) {
            VRL.showMessage('Cannot delete running job.', 'w');
        } else {
            var onDeleteJobResponse = function(response, options) {
                VRL.decodeResponse(response);
                VRL.JobManager.reloadJobs();
            }

            var me = this;
            Ext.Msg.show({
                title: 'Delete Job',
                msg: 'Are you sure you want to delete the selected job and ALL its files?',
                buttons: Ext.Msg.YESNO,
                icon: Ext.Msg.WARNING,
                animEl: 'job-grid',
                closable: false,
                fn: function(btn) {
                    if (btn == 'yes') {
                        VRL.doRequest(me.controllerURL, 'deleteJob',
                            { job: job.id }, onDeleteJobResponse);
                    }
                }
            });
        }
    },

    // issues a Terminate Job request after asking for confirmation
    killJob: function(job) {
        if (Ext.isEmpty(job.submitDate)
                || this.SUBMIT_STATES.indexOf(job.status) >= 0) {
            VRL.showMessage('This job is not running.', 'i');
        } else {
            var onKillJobResponse = function(response, options) {
                VRL.decodeResponse(response);
                VRL.JobManager.reloadJobs();
            }

            var me = this;
            Ext.Msg.show({
                title: 'Kill Job',
                msg: 'Are you sure you want to terminate this job?',
                buttons: Ext.Msg.YESNO,
                icon: Ext.Msg.WARNING,
                closable: false,
                fn: function(btn) {
                    if (btn == 'yes') {
                        VRL.doRequest(me.controllerURL, 'killJob',
                            { job: job.id }, onKillJobResponse);
                    }
                }
            });
        }
    },

    submitJob: function(job) {
        var onSubmit = function() {
            VRL.JobManager.reloadJobs();
        }
        VRL.SubmitJobDialog.show(job, onSubmit);
    },

    updateActions: function(job) {
        this.deleteAction.disable();
        this.editAction.disable();
        this.submitAction.disable();
        this.killAction.disable();
        if (Ext.isObject(job) && !this.seriesReadOnly) {
            this.deleteAction.enable();
            this.editAction.enable();
            if (Ext.isEmpty(job.submitDate)
                    || this.SUBMIT_STATES.indexOf(job.status) >= 0) {
                this.submitAction.enable();
            } else {
                this.killAction.enable();
            }
        }
        this.updateDetailsArea(job);
    },

    updateDetailsArea: function(job) {
        var detailsEl = Ext.getCmp('jm-details-panel').body;
        if (Ext.isObject(job)) {
            VRL.JobManager.jobDetailsTpl.overwrite(detailsEl, job);
        } else {
            detailsEl.update('');
        }
    },

    // creates the job grid with the main functionality of the manager
    createJobGrid: function() {
        this.jobContextMenu = this.jobContextMenu || new Ext.menu.Menu({
            items: [
                this.editAction,
                this.deleteAction
            ]
        });

        var jobStore = new Ext.data.JsonStore({
            url: this.controllerURL,
            baseParams: { 'action': 'listJobs' },
            root: 'jobs',
            sortInfo: { field: 'name', direction: 'ASC' },
            fields: [
                { name: 'id', type: 'long' },
                { name: 'description', type: 'string' },
                { name: 'memory', type: 'int' },
                { name: 'name', type: 'string' },
                { name: 'numProcs', type: 'int' },
                { name: 'queue', type: 'string' },
                { name: 'scriptFile', type: 'string' },
                { name: 'site', type: 'string' },
                { name: 'status', type: 'string' },
                { name: 'stdout', type: 'string' },
                { name: 'stderr', type: 'string' },
                { name: 'submitDate', type: 'long' },
                { name: 'version', type: 'string' },
                { name: 'walltime', type: 'int' }
            ],
            listeners: { 'exception': VRL.onLoadException }
        });

        function dateRenderer(value) {
            if (!Ext.isEmpty(value)) {
                return Ext.util.Format.date(
                    new Date(value), 'd M Y, g:i:s a');
            } else {
                return "Unsubmitted";
            }
        }

        this.jobGrid = new Ext.grid.GridPanel({
            id: 'job-grid',
            region: 'center',
            store: jobStore,
            enableColumnHide: false,
            enableColumnMove: false,
            enableHdMenu: false,
            columns: [
                { header: 'Name', width: 200, sortable: true,
                    dataIndex: 'name' },
                { header: 'Script File', width: 100, sortable: true,
                    dataIndex: 'scriptFile' },
                { header: 'Status', width: 80, sortable: true,
                    dataIndex: 'status' },
                { header: 'Submit Date', width: 150, sortable: true,
                    dataIndex: 'submitDate', renderer: dateRenderer }
            ],
            stripeRows: true,

            menu: this.jobContextMenu,
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true,
                listeners: {
                    'selectionchange': function(sm) {
                        var job;
                        if (sm.getCount() > 0) {
                            job = sm.getSelected().data;
                        }
                        this.updateActions(job);
                    }.createDelegate(VRL.JobManager)
                }
            }),
            tbar: [
                this.newJobAction,
                this.refreshAction,
                '-',
                this.editAction,
                this.deleteAction,
                '-',
                this.submitAction,
                this.killAction
            ],
            listeners: {
                'contextmenu' : function(e) {
                    e.stopEvent();
                },
                'rowcontextmenu': function(grid, rowIndex, e) {
                    if (!grid.getSelectionModel().isSelected(rowIndex)) {
                        grid.getSelectionModel().selectRow(rowIndex);
                    }
                    e.stopEvent();
                    this.menu.showAt(e.getXY());
                },
                'rowdblclick': function(grid, rowIndex, e) {
                    this.editAction.execute();
                }.createDelegate(VRL.JobManager)
            }
        });
    },

    // creates and returns a job manager panel
    create: function() {
        if (!Ext.isObject(this.jobPanel)) {
            this.createJobGrid();
            this.jobsPanel = new Ext.Panel({
                layout: 'border',
                border: false,
                items: [ this.jobGrid, {
                    id: 'jm-details-panel',
                    region: 'south',
                    bodyStyle: 'padding:10px 30px',
                    height: 300,
                    split: true,
                    autoScroll: true,
                    collapsible: true,
                    title: 'Job Details'
                }]
            });
        }
        return this.jobsPanel;
    },

    // reloads job information from server
    reloadJobs: function() {
        if (!this.tStoreProgress) {
            this.tStoreProgress = new Ext.LoadMask('job-grid', {
                msg: "Retrieving job information, please wait...",
                store: this.jobGrid.getStore()
            });
        }
        this.jobGrid.getStore().reload();
    },

    // notifies the job manager of an open/closed series
    setSeries: function(series) {
        if (Ext.isObject(series)) {
            this.seriesReadOnly = series.isExample;
            // do not use reloadJobs() here since we want it to be silent
            this.jobGrid.getStore().reload();
            this.newJobAction.setDisabled(this.seriesReadOnly);
        } else {
            this.jobGrid.getStore().removeAll();
        }
    },
}

