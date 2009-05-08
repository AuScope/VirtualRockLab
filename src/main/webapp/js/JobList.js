
// reference local blank image
Ext.BLANK_IMAGE_URL = 'js/ext/resources/images/default/s.gif';

Ext.namespace('JobList');

JobList.ControllerURL = "joblist.html";

// called when an Ajax request fails
JobList.onRequestFailure = function(response, request) {
    Ext.Msg.alert('Error', 'Could not execute last request. Status: '+
            response.status+' ('+response.statusText+')');
}

// callback for killJob action
JobList.onKillJobResponse = function(response, request) {
    if (response.responseText.error != null) {
        Ext.Msg.alert('Error', response.responseText.error);
    }
    JobList.jobStore.reload();
}

// retrieves filelist of selected job and updates the Details panel
JobList.updateJobDetails = function() {
    var jobGrid = Ext.getCmp('job-grid');
    var descEl = Ext.getCmp('details-tab').body;
    var detailsPanel = Ext.getCmp('details-panel');

    if (jobGrid.getSelectionModel().getSelected()) {
        var jobData = jobGrid.getSelectionModel().getSelected().data;
        JobList.jobFileStore.baseParams.jobId = jobData.id;
        JobList.jobFileStore.reload();
        JobList.jobDescTpl.overwrite(descEl, jobData);
        detailsPanel.enable();
    } else {
        JobList.jobFileStore.removeAll();
        detailsPanel.setActiveTab('details-tab');
        detailsPanel.disable();
    }
}

// retrieves list of jobs of selected series
JobList.updateJobList = function() {
    var jobGrid = Ext.getCmp('job-grid');
    var seriesGrid = Ext.getCmp('series-grid');
    var descEl = Ext.getCmp('details-tab').body;

    if (seriesGrid.getSelectionModel().getSelected()) {
        jobGrid.enable();
        var seriesData = seriesGrid.getSelectionModel().getSelected().data;
        JobList.jobStore.baseParams.seriesId = seriesData.id;
        JobList.jobStore.reload();
        JobList.seriesDescTpl.overwrite(descEl, seriesData);
    } else {
        JobList.jobStore.removeAll();
        jobGrid.disable();
        if (seriesGrid.getStore().getTotalCount() > 0) {
            descEl.update('<p class="jobdesc-title">Please select a series to see its details and the associated jobs.</p>');
        } else {
            descEl.update('<p class="jobdesc-title">No series found.</p>');
        }
    }
}

// (re-)retrieves job details from the server
JobList.refreshAll = function() {
    JobList.seriesStore.reload();
}

// loads series matching given query
JobList.querySeries = function(user, name, desc) {
    if (Ext.isEmpty(user)) {
        JobList.seriesStore.baseParams.qUser = null;
    } else {
        JobList.seriesStore.baseParams.qUser = user;
    }
    if (Ext.isEmpty(name)) {
        JobList.seriesStore.baseParams.qSeriesName = null;
    } else {
        JobList.seriesStore.baseParams.qSeriesName = name;
    }
    if (Ext.isEmpty(desc)) {
        JobList.seriesStore.baseParams.qSeriesDesc = null;
    } else {
        JobList.seriesStore.baseParams.qSeriesDesc = desc;
    }
    JobList.refreshAll();
}

// submits a "kill job" request after asking for confirmation
JobList.killJob = function(jobId) {
    Ext.Msg.confirm('Kill Job', 'Are you sure you want to kill the job?',
        function(btn) {
            if (btn == 'yes') {
                Ext.Ajax.request({
                    url: JobList.ControllerURL,
                    success: JobList.onKillJobResponse,
                    failure: JobList.onRequestFailure, 
                    params: { 'action': 'killJob',
                              'jobId': jobId
                            }
                });
            }
        }
    );
}

JobList.killSeriesJobs = function(seriesId) {
    Ext.Msg.confirm('Kill Series Jobs', 'Are you sure you want to kill ALL jobs in this series?',
        function(btn) {
            if (btn == 'yes') {
                Ext.Ajax.request({
                    url: JobList.ControllerURL,
                    success: JobList.onKillJobResponse,
                    failure: JobList.onRequestFailure, 
                    params: { 'action': 'killSeriesJobs',
                              'seriesId': seriesId
                            }
                });
            }
        }
    );
}

// downloads given file from a specified job
JobList.downloadFile = function(job, file) {
    window.location = JobList.ControllerURL +
        "?action=downloadFile&jobId="+job+"&filename="+file;
}

// downloads a ZIP file containing given files of given job
JobList.downloadMulti = function(job, files) {
    var fparam = files[0].data.name;
    for (var i=1; i<files.length; i++) {
        fparam += ','+files[i].data.name;
    }
    window.location = JobList.ControllerURL +
        "?action=downloadMulti&jobId="+job+"&files="+fparam;
}

JobList.resubmitSeries = function(series) {
    window.location = JobList.ControllerURL +
        "?action=resubmitSeries&seriesId="+series;
}

JobList.resubmitJob = function(job) {
    window.location = JobList.ControllerURL +
        "?action=resubmitJob&jobId="+job;
}

JobList.useScript = function(job, file) {
    window.location = JobList.ControllerURL +
        "?action=useScript&jobId="+job+"&filename="+file;
}

JobList.showQueryDialog = function() {
    var queryForm = new Ext.FormPanel({
        bodyStyle: 'padding:5px;',
        defaults: { anchor: "100%" },
        items: [{
            xtype: 'textfield',
            id: 'qUser',
            fieldLabel: 'User Name'
        }, {
            xtype: 'textfield',
            id: 'qSeriesName',
            fieldLabel: 'Series Name'
        }, {
            xtype: 'textfield',
            id: 'qSeriesDesc',
            fieldLabel: 'Description'
        }]
    });

    var queryWindow = new Ext.Window({
        title: 'Query job series',
        plain: true,
        width: 500,
        resizable: false,
        autoScroll: true,
        constrainHeader: true,
        bodyStyle: 'padding:5px;',
        items: queryForm,
        modal: true,
        buttons: [{
            text: 'Query',
            handler: function() {
                var qUser = Ext.getCmp('qUser').getRawValue();
                var qName = Ext.getCmp('qSeriesName').getRawValue();
                var qDesc = Ext.getCmp('qSeriesDesc').getRawValue();
                JobList.querySeries(qUser, qName, qDesc);
                queryWindow.close();
            }
        }, {
            text: 'Cancel',
            handler: function() { queryWindow.close(); }
        }]
    });

    queryWindow.show();
}

//
// This is the main layout definition.
//
JobList.initialize = function() {
    
    Ext.QuickTips.init();

    JobList.seriesStore = new Ext.data.JsonStore({
        url: JobList.ControllerURL,
        baseParams: { 'action': 'querySeries' },
        root: 'series',
        autoLoad: true,
        fields: [
            { name: 'id', type: 'int' },
            { name: 'name', type: 'string' },
            { name: 'description', type: 'string' },
            { name: 'user', type: 'string'}
        ]
    });

    JobList.seriesStore.on({ 'load': JobList.updateJobList });

    JobList.jobStore = new Ext.data.JsonStore({
        url: JobList.ControllerURL,
        baseParams: { 'action': 'listJobs' },
        root: 'jobs',
        fields: [
            { name: 'id', type: 'int' },
            { name: 'name', type: 'string' },
            { name: 'description', type: 'string' },
            { name: 'site', type: 'string' },
            { name: 'version', type: 'string' },
            { name: 'numTimesteps', type: 'int' },
            { name: 'numParticles', type: 'int' },
            { name: 'numBonds', type: 'int' },
            { name: 'outputDir', type: 'string' },
            { name: 'reference', type: 'string' },
            { name: 'scriptFile', type: 'string' },
            { name: 'status', type: 'string'},
            { name: 'submitDate', type: 'string'}
        ]
    });

    JobList.jobFileStore = new Ext.data.JsonStore({
        url: JobList.ControllerURL,
        baseParams: { 'action': 'jobFiles' },
        root: 'files',
        sortInfo: { field: 'name', direction: 'ASC' },
        fields: [
            { name: 'name', type: 'string' },
            { name: 'size', type: 'int' }
        ]
    });

    //
    // Series Grid & functions
    //
    var seriesGrid = new Ext.grid.GridPanel({
        id: 'series-grid',
        title: 'Series List',
        region: 'center',
        height: 250,
        store: JobList.seriesStore,
        columns: [
            { header: 'User', width: 150, sortable: true, dataIndex: 'user'},
            { header: 'Series Name', sortable: true, dataIndex: 'name'}
        ],
        sm: new Ext.grid.RowSelectionModel({
            singleSelect: true,
            listeners: { 'selectionchange': function(sm) {
                            JobList.updateJobList();
                         }
                       }
        }),
        stripeRows: true
    });

    seriesGrid.on({
        'rowcontextmenu': function(grid, rowIndex, e) {
            grid.getSelectionModel().selectRow(rowIndex);
            var seriesData = grid.getStore().getAt(rowIndex).data;
            if (!this.contextMenu) {
                this.contextMenu = new Ext.menu.Menu({
                    items: [
                        { id: 'rerun-series', text: 'Re-run series' },
                        { id: 'kill-series-jobs', text: 'Kill jobs' }
                    ],
                    listeners: {
                        itemclick: function(item) {
                            switch (item.id) {
                                case 'rerun-series':
                                    JobList.resubmitSeries(seriesData.id);
                                    break;
                                case 'kill-series-jobs':
                                    JobList.killSeriesJobs(seriesData.id);
                                    break;
                            }
                        }
                    }
                });
            }
            e.stopEvent();
            this.contextMenu.showAt(e.getXY());
        }
    });

    //
    // Job Grid & functions
    //
    function jobStatusRenderer(value, cell, record) {
        if (value == "Failed") {
            return '<span style="color:red;">' + value + '</span>';
        } else if (value == "Active") {
            return '<span style="color:green;">' + value + '</span>';
        } else if (value == "Done") {
            return '<span style="color:blue;">' + value + '</span>';
        }
        return value;
    }

    var jobGrid = new Ext.grid.GridPanel({
        id: 'job-grid',
        title: 'Jobs of selected series',
        region: 'south',
        split: true,
        height: 200,
        store: JobList.jobStore,
        columns: [
            { header: 'Job Name', sortable: true, dataIndex: 'name'},
            { header: 'Submit Date', width: 160, sortable: true, dataIndex: 'submitDate'},
            { header: 'Status', sortable: true, dataIndex: 'status', renderer: jobStatusRenderer}
        ],
        sm: new Ext.grid.RowSelectionModel({
            singleSelect: true,
            listeners: { 'selectionchange': function(sm) {
                            JobList.updateJobDetails();
                         }
                       }
        }),
        stripeRows: true
    });

    jobGrid.on({
        'rowcontextmenu': function(grid, rowIndex, e) {
            grid.getSelectionModel().selectRow(rowIndex);
            var jobData = grid.getStore().getAt(rowIndex).data;
            if (!this.contextMenu) {
                this.contextMenu = new Ext.menu.Menu({
                    items: [
                        { id: 'resubmit-job', text: 'Re-submit Job' },
                        { id: 'kill-job', text: 'Kill Job' }
                    ],
                    listeners: {
                        itemclick: function(item) {
                            switch (item.id) {
                                case 'resubmit-job':
                                    JobList.resubmitJob(jobData.id);
                                    break;
                                case 'kill-job':
                                    JobList.killJob(jobData.id);
                                    break;
                            }
                        }
                    }
                });
            }
            e.stopEvent();
            if (jobData.status == 'Active') {
                Ext.getCmp('kill-job').enable();
            } else {
                Ext.getCmp('kill-job').disable();
            }
            this.contextMenu.showAt(e.getXY());
        }
    });

    //
    // File Grid & functions
    //
    function fileTypeRenderer(val) {
        var jobData = jobGrid.getSelectionModel().getSelected().data;
        if (val == jobData.scriptFile) {
            return '<span style="font-weight:bold;">*' + val + '</span>';
        } else if (val == ".py") {
            return '<span style="color:green;">' + val + '</span>';
        } else if (val == "Done") {
            return '<span style="color:blue;">' + val + '</span>';
        }
        return val;
    }

    fileGrid = new Ext.grid.GridPanel({
        id: 'file-grid',
        title: 'Files',
        store: JobList.jobFileStore,
        columns: [
            { header: 'Filename', width: 200, sortable: true, dataIndex: 'name', renderer: fileTypeRenderer},
            { header: 'Size', width: 100, sortable: true, dataIndex: 'size', renderer: Ext.util.Format.fileSize, align: 'right'}
        ],
        stripeRows: true
    });

    function onMenuDownload() {
        var jobData = jobGrid.getSelectionModel().getSelected().data;
        if (fileGrid.getSelectionModel().getCount() == 1) {
            var fileName = fileGrid.getSelectionModel().getSelected().data.name;
            JobList.downloadFile(jobData.id, fileName);
        } else {
            var files = fileGrid.getSelectionModel().getSelections();
            JobList.downloadMulti(jobData.id, files);
        }
    }

    fileGrid.on({
        'rowdblclick': function(grid, rowIndex, e) {
            var jobData = jobGrid.getSelectionModel().getSelected().data;
            var fileName = grid.getStore().getAt(rowIndex).data.name;
            JobList.downloadFile(jobData.id, fileName);
        },
        'rowcontextmenu': function(grid, rowIndex, e) {
            grid.getSelectionModel().selectRow(rowIndex);
            if (!this.contextMenu) {
                this.contextMenu = new Ext.menu.Menu({
                    items: [
                        { id: 'download-file', text: 'Download' },
                        { id: 'use-script', text: 'Use Script' }
                    ],
                    listeners: {
                        itemclick: function(item) {
                            switch (item.id) {
                                case 'download-file':
                                    onMenuDownload();
                                    break;
                                case 'use-script':
                                    var scriptName = grid.getSelectionModel().
                                        getSelected().data.name;
                                    JobList.useScript(jobData.id, scriptName);
                                    break;
                            }
                        }
                    }
                });
            }
            e.stopEvent();
            var jobData = jobGrid.getSelectionModel().getSelected().data;
            if (grid.getStore().getAt(rowIndex).data.name == jobData.scriptFile) {
                Ext.getCmp('use-script').enable();
            } else {
                Ext.getCmp('use-script').disable();
            }
            if (grid.getSelectionModel().getCount() > 1) {
                Ext.getCmp('download-file').setText("Download as Zip");
            } else {
                Ext.getCmp('download-file').setText("Download");
            }
            this.contextMenu.showAt(e.getXY());
        }
    });

    // a template for the job description html area
    JobList.jobDescTpl = new Ext.Template(
        '<p class="jobdesc-title">{name}</p>',
        '<table width="100%"><col width="25%"></col><col class="jobdesc-content"></col>',
        '<tr><td class="jobdesc-key">Submitted on:</td><td>{submitDate}</td></tr>',
        '<tr><td class="jobdesc-key">Computation site:</td><td>{site}</td></tr>',
        '<tr><td class="jobdesc-key">ESyS-Particle version:</td><td>{version}</td></tr>',
        '<tr><td class="jobdesc-key">Input script filename:</td><td>{scriptFile}</td></tr>',
        '<tr><td class="jobdesc-key">Number of timesteps:</td><td>{numTimesteps}</td></tr>',
        '<tr><td class="jobdesc-key">Number of particles:</td><td>{numParticles}</td></tr>',
        '<tr><td class="jobdesc-key">Number of bonds:</td><td>{numBonds}</td></tr></table><br/>',
        '<p class="jobdesc-key">Description:</p><br/><p>{description}</p>'
    );
    JobList.jobDescTpl.compile();

    // a template for the series description html area
    JobList.seriesDescTpl = new Ext.Template(
        '<p class="jobdesc-title">{name}</p><br/>',
        '<p class="jobdesc-key">Description:</p><br/><p>{description}</p>'
    );
    JobList.seriesDescTpl.compile();

    jobDetails = new Ext.TabPanel({
        id: 'details-panel',
        disabled: true,
        activeTab: 0,
        split: true,
        items: [
            {
                title: 'Description',
                bodyStyle: 'padding:10px',
                defaults: { border: false },
                layout: 'fit',
                items: [ { id: 'details-tab', html: '' } ]
            },
            fileGrid
        ]
    });

    new Ext.Viewport({
        layout: 'border',
        items: [{
            xtype: 'box',
            region: 'north',
            applyTo: 'body',
            height: 100
        },{
            id: 'job-panel',
            border: false,
            region: 'west',
            split: true,
            margins: '2 2 2 0',
            layout: 'border',
            width: '400',
            items: [ seriesGrid, jobGrid ]
        },{
            id: 'main-panel',
            title: 'Details',
            region: 'center',
            margins: '2 2 2 0',
            layout: 'fit',
            buttons: [
                { text: 'My Jobs', handler: JobList.querySeries },
                { text: 'Query', handler: JobList.showQueryDialog },
                { text: 'Refresh', handler: JobList.refreshAll }
            ],
            items: [ jobDetails ]
        }]
    });

    if (JobList.error != null) {
        Ext.Msg.alert('Error', JobList.error);
        JobList.error = null;
    }
}


Ext.onReady(JobList.initialize);

