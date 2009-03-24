
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
    var descEl = Ext.getCmp('details-panel').body;

    if (jobGrid.getSelectionModel().getSelected()) {
        var jobData = jobGrid.getSelectionModel().getSelected().data;
        JobList.jobFileStore.baseParams.ref = jobData.reference;
        JobList.jobFileStore.reload();
        JobList.jobDescTpl.overwrite(descEl, jobData);
    } else {
        JobList.jobFileStore.removeAll();
        if (jobGrid.getStore().getTotalCount() > 0) {
            descEl.update("Please select a job to see its details");
        } else {
            descEl.update("");
        }
    }
}

// (re-)retrieves job details from the server
JobList.refreshAll = function() {
    JobList.jobStore.reload();
}

// submits a "kill job" request after asking for confirmation
JobList.killJob = function(ref) {
    Ext.Msg.confirm('Kill Job', 'Are you sure you want to kill the job?',
        function(btn) {
            if (btn == 'yes') {
                Ext.Ajax.request({
                    url: JobList.ControllerURL,
                    success: JobList.onKillJobResponse,
                    failure: JobList.onRequestFailure, 
                    params: { 'action': 'killJob',
                              'ref': ref
                            }
                });
            }
        }
    );
}

// downloads given file from given job reference
JobList.downloadFile = function(ref, file) {
    window.location = JobList.ControllerURL +
        "?action=downloadFile&ref="+ref+"&filename="+file;
}

// downloads a ZIP file containing given files of given job reference
JobList.downloadMulti = function(ref, files) {
    var fparam = files[0].data.name;
    for (var i=1; i<files.length; i++) {
        fparam += ','+files[i].data.name;
    }
    window.location = JobList.ControllerURL +
        "?action=downloadMulti&ref="+ref+"&files="+fparam;
}

JobList.resubmitJob = function(ref) {
    window.location = JobList.ControllerURL + "?action=resubmitJob&ref="+ref;
}

JobList.useScript = function(ref, file) {
    alert("To be implemented");
}

//
// This is the main layout definition.
//
JobList.initialize = function() {
    
    Ext.QuickTips.init();

    JobList.jobStore = new Ext.data.JsonStore({
        url: JobList.ControllerURL,
        baseParams: { 'action': 'listJobs' },
        root: 'model.jobs',
        autoLoad: true,
        fields: [
            { name: 'name', type: 'string' },
            { name: 'outputDir', type: 'string' },
            { name: 'reference', type: 'string' },
            { name: 'scriptFile', type: 'string' },
            { name: 'status', type: 'string'},
            { name: 'timeStamp', type: 'string'},
            { name: 'userId', type: 'string'}
        ]
    });

    JobList.jobStore.on({ 'load': JobList.updateJobDetails });

    JobList.jobFileStore = new Ext.data.JsonStore({
        url: JobList.ControllerURL,
        baseParams: { 'action': 'jobFiles' },
        root: 'model.files',
        sortInfo: { field: 'name', direction: 'ASC' },
        fields: [
            { name: 'name', type: 'string' },
            { name: 'size', type: 'int' }
        ]
    });

    function jobStatusRenderer(val) {
        if (val == "Failed") {
            return '<span style="color:red;">' + val + '</span>';
        } else if (val == "Active") {
            return '<span style="color:green;">' + val + '</span>';
        } else if (val == "Done") {
            return '<span style="color:blue;">' + val + '</span>';
        }
        return val;
    }

    var jobGrid = new Ext.grid.GridPanel({
        id: 'job-grid',
        store: JobList.jobStore,
        columns: [
            { header: 'Job Name', sortable: true, dataIndex: 'name'},
            { header: 'Submit Date', width: 160, sortable: true, dataIndex: 'timeStamp'},
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
                                    JobList.resubmitJob(jobData.reference);
                                    break;
                                case 'kill-job':
                                    JobList.killJob(jobData.reference);
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
            JobList.downloadFile(jobData.reference, fileName);
        } else {
            var files = fileGrid.getSelectionModel().getSelections();
            JobList.downloadMulti(jobData.reference, files);
        }
    }

    fileGrid.on({
        'rowdblclick': function(grid, rowIndex, e) {
            var jobData = jobGrid.getSelectionModel().getSelected().data;
            var fileName = grid.getStore().getAt(rowIndex).data.name;
            JobList.downloadFile(jobData.reference, fileName);
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
                                    JobList.useScript(jobData.reference,
                                        grid.getStore().getAt(rowIndex).data.name);
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
        '<h1 style="font-size:medium; font-style:italic">{name}</h1><br/>',
        '<p>Description:</p><br/><p>{outputDir}</p><br/>',
        '<ul><li>Input script filename: {scriptFile}</li>',
        '<li>Endpoint Reference: {reference}</li></ul>'
    );
    JobList.jobDescTpl.compile();

    jobDetails = new Ext.TabPanel({
        activeTab: 0,
        split: true,
        items: [
            {
                title: 'Details',
                bodyStyle: 'padding:10px',
                defaults: { border: false },
                layout: 'fit',
                items: [ { id: 'details-panel', html: '' } ]
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
            title: 'Job Overview',
            border: false,
            region: 'west',
            split: true,
            margins: '2 2 2 0',
            layout: 'fit',
            width: '400',
            items: [ jobGrid ]
        },{
            id: 'job-details-panel',
            title: 'Job Details',
            region: 'center',
            margins: '2 2 2 0',
            layout: 'fit',
            buttons: [
                { text: 'Refresh', handler: JobList.refreshAll }
            ],
            items: [ jobDetails ]
        }]
    });
}


Ext.onReady(JobList.initialize);

