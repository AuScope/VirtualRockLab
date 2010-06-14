/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.FileManager');

VRL.FileManager = {

    // URL that handles action requests
    controllerURL: 'file.do',

    //
    // FILE MANAGER ACTIONS
    //

    newFileAction: new Ext.Action({
        text: 'New...',
        iconCls: 'new-icon',
        tooltip: 'Creates a new file',
        handler: function() {
            VRL.FileManager.newFileDialog();
        },
        disabled: true
    }),

    refreshAction: new Ext.Action({
        text: 'Refresh',
        iconCls: 'refresh-icon',
        tooltip: 'Reloads the file list from the server',
        handler: function() {
            VRL.FileManager.remoteFileGrid.getStore().reload();
        },
        disabled: true
    }),

    uploadAction: new Ext.Action({
        text: 'Upload',
        iconCls: 'drivedisk-icon',
        tooltip: 'Allows you to upload additional files from your computer',
        handler: function() {
            VRL.FileManager.uploadDialog();
        },
        disabled: true
    }),

    commitChangesAction: new Ext.Action({
        text: 'Commit Changes...',
        iconCls: 'disk-icon',
        tooltip: 'Saves modifications under a new series revision',
        handler: function() {
            VRL.FileManager.commitChanges();
        },
        disabled: true
    }),

    //
    // EDITOR ACTIONS
    //
 
    closeFileAction: new Ext.Action({
        text: 'Close',
        iconCls: 'cross-icon',
        handler: function() {
            var tab = Ext.getCmp('editor-tab').getActiveTab();
            if (!VRL.FileManager.seriesReadOnly &&
                    tab.items.first().isDirty()) {
                Ext.Msg.show({
                    title: 'Close File',
                    msg: 'Save changes before closing?',
                    buttons: Ext.Msg.YESNOCANCEL,
                    icon: Ext.Msg.QUESTION,
                    closable: false,
                    fn: function(btn) {
                        if (btn == 'yes') {
                            this.saveFileAction.execute();
                        }
                        if (btn != 'cancel') {
                            Ext.getCmp('editor-tab').remove(tab);
                            if (Ext.getCmp('editor-tab').items.getCount()
                                    == 0) {
                                VRL.activateTab('files-tab');
                            }
                        }
                    }.createDelegate(VRL.FileManager)
                });
            } else {
                Ext.getCmp('editor-tab').remove(tab);
                if (Ext.getCmp('editor-tab').items.getCount()
                        == 0) {
                    VRL.activateTab('files-tab');
                }
            }
        }
    }),

    revertFileAction: new Ext.Action({
        text: 'Revert',
        iconCls: 'refresh-icon',
        handler: function() {
            var tab = Ext.getCmp('editor-tab').getActiveTab();
            var editor = tab.items.first();
            if (editor.isDirty()) {
                Ext.Msg.show({
                    title: 'Revert File',
                    msg: 'Discard changes and revert to saved version?',
                    buttons: Ext.Msg.YESNO,
                    icon: Ext.Msg.QUESTION,
                    closable: false,
                    fn: function(btn) {
                        if (btn == 'yes') {
                            editor.reset();
                        }
                    }
                });
            }
        }
    }),

    saveFileAction: new Ext.Action({
        text: 'Save',
        iconCls: 'save-icon',
        handler: function() {
            var tab = Ext.getCmp('editor-tab').getActiveTab();
            var editor = tab.items.first();
            if (editor.isDirty()) {
                var content = editor.getValue();
                VRL.FileManager.saveFile(
                    tab.job, tab.fileName, content);
            }
        }
    }),

    //
    // FUNCTIONS
    //

    // Requests to copy the remote files to local directory
    copyFilesToInput: function(jobId, files) {
        var onCopyFilesResponse = function(response, options) {
            VRL.decodeResponse(response);
            this.localFileGrid.getStore().reload();
        }

        var fparam = files[0].data.name;
        for (var i=1; i<files.length; i++) {
            fparam += ','+files[i].data.name;
        }

        VRL.doRequest(this.controllerURL, 'copyFiles',
            { files:fparam, srcJob:jobId, destJob:jobId, overwrite:'on' },
            onCopyFilesResponse.createDelegate(this));
    },

    // Shows a job selector to copy files to that job
    copyFilesToJob: function(jobId, files) {
        var fparam = files[0].data.name;
        for (var i=1; i<files.length; i++) {
            fparam += ','+files[i].data.name;
        }

        var onCopySuccess = function(form, action) {
            if (!Ext.isEmpty(action.result.error)) {
                VRL.showError('Error copying file(s): '
                        + action.result.error);
            } else {
                this.localFileGrid.getStore().reload();
                Ext.getCmp('cft-win').close();
            }
        }

        var onCopyFailure = function(form, action) {
            VRL.showError('Error copying file(s): '
                    + action.result.error);
        }

        var doCopy = function() {
            Ext.getCmp('cft-form').getForm().submit({
                url: this.controllerURL,
                params: { 'action': 'copyFiles' },
                success: onCopySuccess,
                failure: onCopyFailure,
                scope: this,
                waitMsg: 'Copying files, please wait...',
                waitTitle: 'File copy in progress'
            });
        }

        var w = new Ext.Window({
            title: 'Copy File(s) to Job',
            id: 'cft-win',
            layout: 'fit',
            closable: false,
            resizable: false,
            constrain: true,
            constrainHeader: true,
            modal: true,
            width: 350,
            height: 130,
            border: false,
            footer: true,
            initHidden: false,
            items: [{
                xtype: 'form',
                id: 'cft-form',
                frame: true,
                labelWidth: 120,
                items: [{
                    xtype: 'combo',
                    hiddenName: 'destJob',
                    allowBlank: false,
                    displayField: 'name',
                    editable: false,
                    emptyText: 'Select a job',
                    fieldLabel: 'Destination Job',
                    mode: 'local',
                    store: VRL.JobManager.jobGrid.getStore(),
                    triggerAction: 'all',
                    valueField: 'id',
                    anchor: '100%'
                },{
                    xtype: 'checkbox',
                    name: 'overwrite',
                    fieldLabel: 'Overwrite Files',
                    anchor: '100%'
                },{
                    xtype: 'hidden',
                    name: 'srcJob',
                    value: jobId
                },{
                    xtype: 'hidden',
                    name: 'files',
                    value: fparam
                }]
            }],
            fbar: new Ext.Toolbar({
                items: [{
                    text: 'Cancel',
                    handler: function() { w.close(); }
                }, {
                    text:'OK',
                    handler: function() {
                        if (Ext.getCmp('cft-form').getForm().isValid()) {
                            doCopy.apply(this);
                        } else {
                            Ext.Msg.alert('No job selected',
                             'Please select the job to copy the files to.');
                        }
                    }.createDelegate(VRL.FileManager)
                }]
            }),
        });
    },

    // issues a Delete Files request after asking for confirmation
    deleteFiles: function(job, files) {
        var onDeleteFilesResponse = function(response, options) {
            VRL.decodeResponse(response);
            this.localFileGrid.getStore().reload();
        }

        var me = this;
        var i = 0;

        // ignore deleted files
        while (i<files.length && files[i].data.state === 'D') {
            i++;
        }
        if (i==files.length) {
            return;
        }
        var fparam = files[i].data.name;

        Ext.Msg.show({
            title: 'Delete Files',
            msg: 'Are you sure you want to delete the selected file(s)?',
            buttons: Ext.Msg.YESNO,
            icon: Ext.Msg.WARNING,
            animEl: 'file-grid',
            closable: false,
            fn: function(btn) {
                if (btn == 'yes') {
                    // close editor(s)
                    var title = me.editorTitle(job, fparam);
                    var arr = Ext.getCmp('editor-tab').find('title', title);
                    if (arr.length > 0) {
                        Ext.getCmp('editor-tab').remove(arr[0]);
                    }
                    for (i=i+1; i<files.length; i++) {
                        if (files[i].data.state !== 'D') {
                            var filename = files[i].data.name;
                            var title = me.editorTitle(job, filename);
                            fparam += ','+filename;
                            arr = Ext.getCmp('editor-tab').find('title', title);
                            if (arr.length > 0) {
                                Ext.getCmp('editor-tab').remove(arr[0]);
                            }
                        }
                    }
                    VRL.doRequest(me.controllerURL, 'deleteFiles',
                        { job: job.get('id'), files: fparam },
                        onDeleteFilesResponse.createDelegate(
                            VRL.FileManager)
                    );
                }
            }
        });
    },

    // issues a Download Files request
    downloadFiles: function(jobId, files, remote) {
        var i=0;
        while (i<files.length && files[i].data.state === 'D') {
            i++;
        }
        if (i==files.length) {
            return;
        }
        var fparam = files[i].data.name;
        for (i=i+1; i<files.length; i++) {
            if (files[i].data.state !== 'D') {
                fparam += ','+files[i].data.name;
            }
        }
        var iframe = document.createElement("iframe");
        iframe.style.display = "none";
        iframe.src = this.controllerURL
            + '?action=downloadFiles&job=' + jobId + '&files=' + fparam;
        if (remote) {
            iframe.src += '&remote=1';
        }
        document.body.appendChild(iframe);
    },

    // creates a window/tab title for given job and filename
    editorTitle: function(job, filename) {
        return job.get('name')+':'+filename;
    },

    // issues Get File Contents requests for all given filenames
    openFiles: function(job, files) {
        var onOpenFilesResponse = function(response, options) {
            var resp = VRL.decodeResponse(response);
            if (resp) {
                this.createFileTab(job, resp.fileName, resp.sourceText);
            }
        }

        var first = true;

        for (var i=0; i<files.length; i++) {
            if (files[i].data.state === 'D') {
                continue;
            }
            if (first) {
                VRL.activateTab('editor-tab');
                first = false;
            }

            var file = files[i].data.name;

            // look if file is already open in a tab
            var title = this.editorTitle(job, file);
            var arr = Ext.getCmp('editor-tab').find('title', title);
            if (arr.length > 0) {
                Ext.getCmp('editor-tab').setActiveTab(arr[0]);
            } else {
                // file is not open yet so request the contents
                VRL.doRequest(this.controllerURL, 'getFileContents',
                    { filename: file, job: job.get('id') },
                    onOpenFilesResponse.createDelegate(this));
            }
        }
    },

    // issues a Revert Files request after asking for confirmation
    revertFiles: function(job, files) {
        var onRevertFilesResponse = function(response, options) {
            VRL.decodeResponse(response);
            this.localFileGrid.getStore().reload();
        }

        var me = this;
        var i = 0;

        // ignore unmodified files
        while (i<files.length && files[i].data.state === 'N') {
            i++;
        }
        if (i==files.length) {
            return;
        }
        var fparam = files[i].data.name;

        Ext.Msg.show({
            title: 'Revert Files',
            msg: 'Are you sure you want to revert the selected file(s)?',
            buttons: Ext.Msg.YESNO,
            icon: Ext.Msg.WARNING,
            animEl: 'file-grid',
            closable: false,
            fn: function(btn) {
                if (btn == 'yes') {
                    // close editor
                    var title = me.editorTitle(job, fparam);
                    var arr = Ext.getCmp('editor-tab').find('title', title);
                    if (arr.length > 0) {
                        Ext.getCmp('editor-tab').remove(arr[0]);
                    }
                    for (i=i+1; i<files.length; i++) {
                        if (files[i].data.state !== 'N') {
                            var filename = files[i].data.name;
                            var title = me.editorTitle(job, filename);
                            fparam += ','+filename;
                            arr = Ext.getCmp('editor-tab').find('title', title);
                            if (arr.length > 0) {
                                Ext.getCmp('editor-tab').remove(arr[0]);
                            }
                        }
                    }
                    VRL.doRequest(me.controllerURL, 'revertFiles',
                        { job: job.get('id'), files: fparam },
                        onRevertFilesResponse.createDelegate(VRL.FileManager)
                    );
                }
            }
        });
    },

    // issues a Save File request
    saveFile: function(jobId, filename, content) {
        var onSaveFileResponse = function(response, options) {
            VRL.decodeResponse(response);
            this.localFileGrid.getStore().reload();
            var tab = Ext.getCmp('editor-tab').getActiveTab();
            var editor = tab.items.first();
            editor.originalValue = editor.getValue();
        }

        VRL.doRequest(this.controllerURL, 'saveFileContents',
            { job: jobId, filename: filename, contents: content },
            onSaveFileResponse.createDelegate(this));
    },

    // displays the New File dialog
    newFileDialog: function() {
        var w = new Ext.Window({
            title: 'New File',
            layout: 'form',
            labelWidth: 60,
            closable: false,
            resizable: false,
            constrain: true,
            constrainHeader: true,
            modal: true,
            bodyStyle: 'padding:5px',
            buttonAlign: 'right',
            width: 300,
            height: 100,
            initHidden: false,
            plain: true,
            footer: true,
            fbar: new Ext.Toolbar({
                items: [{
                    text: 'Cancel',
                    handler: function() { w.close(); }
                }, {
                    text:'OK',
                    handler: function() {
                        var f = Ext.getCmp('nf-filename');
                        // look if file already exists
                        var title = this.editorTitle(this.job, f.getValue());
                        var arr = Ext.getCmp('editor-tab').find('title', title);
                        if (arr.length > 0) {
                            VRL.activateTab('editor-tab');
                            Ext.getCmp('editor-tab').setActiveTab(arr[0]);
                            w.close();
                        } else if (this.localFileGrid.getStore().find(
                                'name', f.getValue()) >= 0) {
                            VRL.showError(
                                'A file by that name already exists.', null, 1);
                        } else if (f.isValid()) {
                            VRL.activateTab('editor-tab');
                            this.createFileTab(this.job, f.getValue(), '');
                            w.close();
                        } else {
                            VRL.showError(
                                'The filename is not valid.', null, 1);
                        }
                    }.createDelegate(VRL.FileManager)
                }]
            }),
            items: [{
                fieldLabel: 'Filename',
                id: 'nf-filename',
                xtype: 'textfield',
                anchor: '100%',
                allowBlank: false,
                maskRe: /[\w+#@.,-]/,
                regex: /^([\w]+)([\w+#@.,-])*$/
            }]
        });
    },

    // displays the File Upload dialog
    uploadDialog: function() {
        var onUploadSuccess = function(form, action) {
            if (!Ext.isEmpty(action.result.error)) {
                VRL.showError('Error uploading file: '
                        + action.result.error);
            } else {
                this.localFileGrid.getStore().reload();
                Ext.getCmp('uf-win').close();
            }
        }

        var onUploadFailure = function(form, action) {
            VRL.showError('Error uploading file: '
                    + action.result.error);
        }

        var doUpload = function() {
            Ext.getCmp('uf-form').getForm().submit({
                url: this.controllerURL,
                params: { 'action': 'uploadFile', 'job': this.job.get('id') },
                success: onUploadSuccess,
                failure: onUploadFailure,
                scope: this,
                waitMsg: 'Sending file, please wait...',
                waitTitle: 'Upload in progress'
            });
        }

        var w = new Ext.Window({
            title: 'Upload File',
            id: 'uf-win',
            layout: 'fit',
            closable: false,
            resizable: false,
            constrain: true,
            constrainHeader: true,
            modal: true,
            width: 300,
            height: 100,
            border: false,
            footer: true,
            initHidden: false,
            items: [{
                xtype: 'form',
                id: 'uf-form',
                fileUpload: true,
                frame: true,
                hideLabels: true,
                items: {
                    xtype: 'fileuploadfield',
                    id: 'uf-file',
                    allowBlank: false,
                    emptyText: 'Select a file to upload',
                    name: 'file',
                    anchor: '100%'
                }
            }],
            fbar: new Ext.Toolbar({
                items: [{
                    text: 'Cancel',
                    handler: function() { w.close(); }
                }, {
                    text:'Upload',
                    handler: function() {
                        if (Ext.getCmp('uf-form').getForm().isValid()) {
                            var ufName = Ext.getCmp('uf-file').getValue();
                            if (this.localFileGrid.getStore().find(
                                    'name', ufName) >= 0) {
                                Ext.Msg.confirm('File exists',
                                    'A file by that name already exists. Overwrite?',
                                    function(btn) {
                                        if (btn === 'yes') {
                                            doUpload.apply(VRL.FileManager);
                                        }
                                    }
                                );
                            } else {
                                doUpload.apply(this);
                            }
                        } else {
                            Ext.Msg.alert('No file selected',
                             'Please use the browse button to select a file.');
                        }
                    }.createDelegate(VRL.FileManager)
                }]
            }),
        });
    },

    // creates a new file editor tab
    createFileTab: function(job, filename, content) {
        var lang = (filename.lastIndexOf('.py') == filename.length-3 ?
                'python' : 'text');
        var tab = Ext.getCmp('editor-tab').add({
            title: this.editorTitle(job, filename),
            fileName: filename,
            job: job.get('id'),
            iconCls: 'edit-icon',
            border: true,
            layout: 'fit',
            items: {
                bodyStyle: 'text-align:left;padding:5px',
                xtype: 'ux-codemirror',
                language: lang,
                readOnly: this.seriesReadOnly,
                value: content
            },
            tbar: [
                this.saveFileAction,
                this.revertFileAction,
                this.closeFileAction
            ]
        });
        Ext.getCmp('editor-tab').setActiveTab(tab);
    },

    removeFileTabs: function() {
        Ext.getCmp('editor-tab').removeAll();
    },

    // creates the file grid for local files
    createJobChooserPanel: function() {
        this.jobChooserPanel = new Ext.Panel({
            id: 'fm-jobchooser',
            region: 'north',
            height: 36,
            bodyStyle: 'padding:8px',
            layout: 'form',
            defaults: { anchor: '100%' },
            labelWidth: 140,
            items: [{
                xtype: 'combo',
                id: 'fm-jobcombo',
                editable: false,
                mode: 'local',
                store: VRL.JobManager.jobGrid.getStore(),
                displayField: 'name',
                triggerAction: 'all',
                fieldLabel: 'Display Files For Job',
                listeners: {
                    scope: this,
                    'select': function(combo, record, index) {
                        this.job=record;
                        this.reloadFileList();
                        if (!this.seriesReadOnly) {
                            this.commitChangesAction.enable();
                            this.newFileAction.enable();
                            this.refreshAction.enable();
                            this.uploadAction.enable();
                        }
                    }
                }
            }]
        });
    },

    commitChanges: function() {
        var callback = function(seriesDetails) {
            Ext.Msg.alert('New revision saved',
                'Revision '+seriesDetails.revision+' saved successfully.');
            Ext.apply(VRL.currentSeries, seriesDetails);
            VRL.updateSeriesSummary();
            VRL.FileManager.reloadFileList();
        }
        VRL.SaveSeriesDialog.show(callback);
    },

    checkForModifications: function(callback, scope) {
        var onResponse = function(response, options) {
            var resp = VRL.decodeResponse(response);
            var modified = (resp ? resp.modified : true);
            if (Ext.isFunction(callback)) {
                callback.call(scope || this, modified);
            }
        }
        VRL.doRequest(this.controllerURL, 'isModified', {}, onResponse);
    },

    // creates the file grid for local files
    createLocalFileGrid: function() {
        // actions
        var deleteFilesAction = new Ext.Action({
            text: 'Delete',
            iconCls: 'deletefile-icon',
            tooltip: 'Deletes the selected file(s)',
            handler: function() {
                var job = VRL.FileManager.job;
                var files = VRL.FileManager.localFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.deleteFiles(job, files);
            },
            disabled: true
        });

        var downloadAction = new Ext.Action({
            text: 'Download',
            iconCls: 'compressed-icon',
            tooltip: 'Download the selected file(s) to your computer',
            handler: function() {
                var jobId = VRL.FileManager.job.get('id');
                var files = VRL.FileManager.localFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.downloadFiles(jobId, files, false);
            },
            disabled: true
        });

        var editAction = new Ext.Action({
            text: 'Edit',
            iconCls: 'edit-icon',
            tooltip: 'Opens the file(s) in an editor within the browser',
            handler: function() {
                var job = VRL.FileManager.job;
                var files = VRL.FileManager.localFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.openFiles(job, files);
            },
            disabled: true
        });

        var revertFilesAction = new Ext.Action({
            text: 'Revert',
            iconCls: 'refresh-icon',
            tooltip: 'Restores the selected file(s)',
            handler: function() {
                var job = VRL.FileManager.job;
                var files = VRL.FileManager.localFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.revertFiles(job, files);
            },
            disabled: true
        });

        var fileContextMenu = new Ext.menu.Menu({
            items: [
                editAction,
                deleteFilesAction,
                revertFilesAction,
                downloadAction
            ]
        });

        function fileNameRenderer(value, cell, record) {
            var styleStr = '';
            // weight
            if (record.get('state') !== 'N') {
                styleStr += 'font-weight:bold;';
            }
            // decoration
            if (record.get('state') === 'D') {
                styleStr += 'text-decoration:line-through;';
            }
            // color
            if (value.lastIndexOf('.py') == value.length-3) {
                styleStr += 'color:green;';
            }
            // style
            var jobs = VRL.JobManager.jobGrid.getStore();
            if (jobs.find('scriptFile', value) >=0) {
                styleStr += 'font-style:italic;';
            }
            return '<span style="' + styleStr + '">' + value + '</span>';
        }

        function fileStatusRenderer(value, cell, record) {
            if (value === 'M') {
                return '<span style="font-weight:bold;">Modified</span>';
            } else if (value === 'A') {
                return '<span style="color:blue">New File</span>';
            } else if (value === 'D') {
                return '<span style="color:red">Deleted</span>';
            } else if (value === 'N') {
                return 'Normal';
            }
            return value;
        }

        var localFileStore = new Ext.data.JsonStore({
            url: this.controllerURL,
            baseParams: { 'action': 'listFiles' },
            root: 'files',
            sortInfo: { field: 'name', direction: 'ASC' },
            fields: [
                { name: 'name', type: 'string' },
                { name: 'size', type: 'int' },
                { name: 'state', type: 'string' }
            ],
            listeners: { 'exception': VRL.onLoadException }
        });

        this.localFileGrid = new Ext.grid.GridPanel({
            id: 'file-grid',
            title: 'Local Files',
            region: 'west',
            width: '50%',
            split: true,
            store: localFileStore,
            enableColumnHide: false,
            enableColumnMove: false,
            enableHdMenu: false,
            columns: [
                { header: 'Filename', width: 200, sortable: true,
                    dataIndex: 'name', renderer: fileNameRenderer },
                { header: 'Size', width: 100, sortable: true, dataIndex: 'size',
                    renderer: Ext.util.Format.fileSize, align: 'right'},
                { header: 'Status', width: 100, sortable: true,
                    dataIndex: 'state', renderer: fileStatusRenderer }
            ],
            stripeRows: true,

            menu: fileContextMenu,
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: false,
                listeners: {
                    scope: this,
                    'selectionchange': function(sm) {
                        if (sm.getCount() == 0) {
                            deleteFilesAction.disable();
                            downloadAction.disable();
                            editAction.disable();
                            revertFilesAction.disable();
                        } else {
                            var files = sm.getSelections();
                            var canEdit = false, canRevert = false;
                            for (var i=0; i<files.length; i++) {
                                if (files[i].data.state !== 'D') {
                                    canEdit = true;
                                }
                                if (files[i].data.state !== 'N') {
                                    canRevert = true;
                                }
                            }
                            if (canEdit) {
                                downloadAction.enable();
                                editAction.enable();
                                if (!this.seriesReadOnly) {
                                    deleteFilesAction.enable();
                                }
                            }
                            if (canRevert && !this.seriesReadOnly) {
                                revertFilesAction.enable();
                            }
                        }
                    }
                }
            }),
            tbar: [
                this.newFileAction,
                editAction,
                deleteFilesAction,
                revertFilesAction,
                downloadAction,
                this.uploadAction,
                '->',
                this.commitChangesAction
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
                    editAction.execute();
                }
            }
        });
    },

    // creates the file grid for remote files
    createRemoteFileGrid: function() {
        // actions
        var downloadAction = new Ext.Action({
            text: 'Download',
            iconCls: 'compressed-icon',
            tooltip: 'Download the selected file(s) to your computer',
            handler: function() {
                var jobId = VRL.FileManager.job.get('id');
                var files = VRL.FileManager.remoteFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.downloadFiles(jobId, files, true);
            },
            disabled: true
        });
        var copyToInputAction = new Ext.Action({
            text: 'Copy to input',
            iconCls: 'arrowleft-icon',
            tooltip: 'Copies the selected file(s) to the input directory',
            handler: function() {
                var jobId = VRL.FileManager.job.get('id');
                var files = VRL.FileManager.remoteFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.copyFilesToInput(jobId, files);
            },
            disabled: true
        });
        var copyToJobAction = new Ext.Action({
            text: 'Copy to...',
            iconCls: 'copy-icon',
            tooltip: 'Copies the selected file(s) to a different job',
            handler: function() {
                var jobId = VRL.FileManager.job.get('id');
                var files = VRL.FileManager.remoteFileGrid
                    .getSelectionModel().getSelections();
                VRL.FileManager.copyFilesToJob(jobId, files);
            },
            disabled: true
        });

        var fileContextMenu = new Ext.menu.Menu({
            items: [
                downloadAction,
                copyToInputAction,
                copyToJobAction
            ]
        });

        function fileTypeRenderer(value, cell, record) {
            var styleStr = '';
            // color
            if (value.lastIndexOf('.py') == value.length-3) {
                styleStr += 'color:green;';
            }
            // style
            var jobs = VRL.JobManager.jobGrid.getStore();
            if (jobs.find('scriptFile', value) >=0) {
                styleStr += 'font-style:italic;';
            }
            return '<span style="' + styleStr + '">' + value + '</span>';
        }

        var remoteFileStore = new Ext.data.JsonStore({
            url: this.controllerURL,
            baseParams: { 'action': 'listFiles', 'remote': 1 },
            root: 'files',
            sortInfo: { field: 'name', direction: 'ASC' },
            fields: [
                { name: 'name', type: 'string' },
                { name: 'size', type: 'int' }
            ],
            listeners: { 'exception': VRL.onLoadException }
        });

        this.remoteFileGrid = new Ext.grid.GridPanel({
            id: 'rfile-grid',
            title: 'Remote Files',
            region: 'center',
            split: true,
            store: remoteFileStore,
            enableColumnHide: false,
            enableColumnMove: false,
            enableHdMenu: false,
            columns: [
                { header: 'Filename', width: 200, sortable: true,
                    dataIndex: 'name', renderer: fileTypeRenderer },
                { header: 'Size', width: 100, sortable: true, dataIndex: 'size',
                    renderer: Ext.util.Format.fileSize, align: 'right'}
            ],
            stripeRows: true,

            menu: fileContextMenu,
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: false,
                listeners: {
                    scope: this,
                    'selectionchange': function(sm) {
                        if (sm.getCount() == 0) {
                            copyToInputAction.disable();
                            downloadAction.disable();
                        } else {
                            copyToInputAction.enable();
                            copyToJobAction.enable();
                            downloadAction.enable();
                        }
                    }
                }
            }),
            tbar: [
                this.refreshAction,
                downloadAction,
                copyToInputAction,
                copyToJobAction
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
                    downloadAction.execute();
                }
            }
        });
    },

    // creates and returns a file manager panel
    create: function() {
        if (!Ext.isObject(this.filesPanel)) {
            this.createJobChooserPanel();
            this.createLocalFileGrid();
            this.createRemoteFileGrid();
            this.filesPanel = new Ext.Panel({
                layout: 'border',
                border: false,
                items: [
                    this.jobChooserPanel,
                    this.localFileGrid,
                    this.remoteFileGrid
                ]
            });
        }
        return this.filesPanel;
    },

    reloadFileList: function() {
        if (!this.rStoreProgress) {
            this.rStoreProgress = new Ext.LoadMask('rfile-grid', {
                msg: "Retrieving file list, please wait...",
                store: this.remoteFileGrid.getStore()
            });
        }
        if (!this.lStoreProgress) {
            this.lStoreProgress = new Ext.LoadMask('file-grid', {
                msg: "Retrieving file list, please wait...",
                store: this.localFileGrid.getStore()
            });
        }
        this.localFileGrid.getStore().removeAll();
        this.localFileGrid.getStore().baseParams.job = this.job.get('id');
        this.localFileGrid.getStore().reload();
        this.remoteFileGrid.getStore().removeAll();
        this.remoteFileGrid.getStore().baseParams.job = this.job.get('id');
        this.remoteFileGrid.getStore().reload();
    },

    // notifies the file manager of an open/closed series
    setSeries: function(series) {
        if (Ext.isObject(series)) {
            this.seriesReadOnly = series.isExample;
            this.commitChangesAction.disable();
            this.newFileAction.disable();
            this.uploadAction.disable();
            this.revertFileAction.setDisabled(this.seriesReadOnly);
            this.saveFileAction.setDisabled(this.seriesReadOnly);
            this.job = undefined;
            Ext.getCmp('fm-jobcombo').setValue();
        } else {
            this.removeFileTabs();
        }
        this.localFileGrid.getStore().removeAll();
        this.remoteFileGrid.getStore().removeAll();
    }
}

