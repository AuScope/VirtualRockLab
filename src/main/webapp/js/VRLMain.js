/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

// reference local blank image
Ext.BLANK_IMAGE_URL = 'js/ext/resources/images/default/s.gif';

Ext.namespace('VRL');

Ext.apply(VRL, {

controllerURL: 'series.do',

// called when a JsonStore fails to retrieve data from the server
onLoadException: function(proxy, type, action, options, response, arg) {
    VRL.hideProgressDlg();
    if (type === 'remote') {
        var msg='Could not retrieve data from the server.';
        if (Ext.isObject(response.raw) && !Ext.isEmpty(response.raw.error)) {
            msg+=' ('+response.raw.error+')';
        }
        VRL.showMessage(msg);
    } else /* type === 'response' */ {
        VRL.showMessage('Communication error (' + response.statusText
            + '). Maybe your session has expired. '
            + 'Please try reloading the page or contact our staff.');
    }
},

// called when the user tries to navigate away from this site
onWindowUnloading: function(e) {
    if (Ext.isObject(VRL.currentSeries)) {
        e.browserEvent.returnValue =
            'There is an open series. Unsaved changes will be lost!';
    }
},

// decodes the server response and checks the error parameter.
// Displays an error dialog on failure, returns the response object on success.
decodeResponse: function(response, callback) {
    VRL.hideProgressDlg();
    var resp = Ext.decode(response.responseText);
    if (resp.error != null) {
        VRL.showMessage(resp.error+'.', 'e', callback);
        return;
    }
    return resp;
},

// shows a message dialog with given message
showMessage: function(message, type, callback, scope) {
    var t, i;
    if (type === 'i') {
        t = 'Information';
        i = Ext.Msg.INFO;
    } else if (type === 'w') {
        t = 'Warning';
        i = Ext.Msg.WARNING;
    } else {
        t = 'Error';
        i = Ext.Msg.ERROR;
    }
    VRL.msgDlg = Ext.Msg.show({
        title: t,
        msg: message,
        buttons: Ext.Msg.OK,
        icon: i,
        fn: function() {
            VRL.msgDlg = undefined;
            if (Ext.isFunction(callback)) { callback.call(scope?scope:this); }
        }
    });
},

progressDlg: new Ext.LoadMask(Ext.getBody(), {
        msg:"Transferring data, please wait..."
    }
),

// hides the data retrieval progress dialog
hideProgressDlg: function() {
    if (!VRL.progressDlg.disabled) {
        VRL.progressDlg.hide();
        VRL.progressDlg.disable();
    }
},

// notifies the user through a progress dialog that data is being retrieved
showProgressDlg: function() {
    if (VRL.progressDlg.disabled && !VRL.msgDlg) {
        VRL.progressDlg.enable();
        VRL.progressDlg.show();
    }
},

// executes an Ajax request.
// url - controller URL, action - name of action, params - action parameters,
// sCallback - success callback, fCallback - failure callback (optional)
doRequest: function(url, action, params, sCallback, fCallback) {
    var onRequestFailure = function(response, options) {
        VRL.hideProgressDlg();
        VRL.showMessage('Could not execute last request. Status: '
            + response.status + ' (' + response.statusText + ')');
    }

    var fcbk = fCallback || onRequestFailure;
    var p = Ext.apply({ 'action': action }, params);
    VRL.showProgressDlg();
    Ext.Ajax.request({
        url: url,
        success: sCallback,
        failure: fcbk,
        timeout: 60000, // allow 60 seconds for requests
        params: p
    });
},

updateSeriesSummary: function() {
    var descEl = Ext.getCmp('seriesdesc-panel').body;
    VRL.seriesDescTpl.overwrite(descEl, VRL.currentSeries);
},

openSeries: function(seriesId, revision) {
    function doOpen() {
        VRL.doRequest(this.controllerURL, 'openSeries',
            { seriesId: seriesId, revision: revision },
            function(response, options) {
                var resp = VRL.decodeResponse(response, VRL.showWelcome);
                if (!resp) {
                    return;
                }

                VRL.currentSeries = resp;
                VRL.updateSeriesSummary();
                Ext.getCmp('main-tabpanel').show();
                VRL.FileManager.setSeries(resp);
                VRL.JobManager.setSeries(resp);
            }
        );
    }

    if (Ext.isObject(this.currentSeries)) {
        this.closeSeries(doOpen, this);
    } else {
        doOpen.call(this);
    }
},

cloneSeries: function() {
    VRL.NewSeriesDialog.show(VRL.currentSeries);
},

closeSeries: function(callback, scope) {
    var doClose = function() {
        VRL.doRequest(VRL.controllerURL, 'closeSeries', {},
            function(response, options) {
                var resp=VRL.decodeResponse(response, VRL.showWelcome);
                Ext.getCmp('main-tabpanel').hide();
                VRL.currentSeries = undefined;
                VRL.FileManager.setSeries(undefined);
                VRL.JobManager.setSeries(undefined);
                // if there was an error, the welcome screen will be shown
                // after the user dismisses the error dialog
                if (resp) {
                    if (Ext.isFunction(callback)) {
                        callback.call(scope || this);
                    } else {
                        VRL.showWelcome();
                    }
                }
            }
        );
    }

    var checkResult = function(modified) {
        if (modified) {
            Ext.Msg.confirm('Unsaved Files',
                'Unsaved changes will be lost! Close anyway?',
                function(btn) {
                    if (btn === 'yes') {
                        doClose();
                    }
                }
            );
        } else {
            doClose();
        }
    }

    VRL.FileManager.checkForModifications(checkResult, this);
},

// shows welcome window
showWelcome: function() {
    var browserCb = function(attributes) {
        if (attributes) {
            var seriesId = attributes.seriesId;
            var revision = attributes.revision;
            VRL.openSeries(seriesId, revision);
        } else {
            VRL.showWelcome();
        }
    }
 
    this.WelcomeDialog.show(function(choice) {
        switch (choice) {
            case 1: {
                VRL.NewSeriesDialog.show();
                break;
            }
            case 2: {
                VRL.SeriesBrowser.show(browserCb, false);
                break;
            }
            case 3: {
                VRL.SeriesBrowser.show(browserCb, true);
                break;
            }
        }
    });
},

activateTab: function(tab) {
    Ext.getCmp('main-tabpanel').activeGroup.setActiveTab(Ext.getCmp(tab));
},

/////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////

initialize: function() {
    
    Ext.QuickTips.init();

    // a template for the series description html area
    this.seriesDescTpl = new Ext.Template(
        '<p class="seriesdesc-title">{name}</p>',
        '<table class="seriesdesc">',
        '<tr><th>Description:</th><td>{description}</td></tr>',
        '<tr><th>Created on:</th><td>{creationDate:this.dateFmt}</td></tr>',
        '<tr><th>Last Modified:</th><td>{lastModified:this.dateFmt}</td></tr>',
        '<tr><th>Latest Revision:</th><td>{latestRevision}</td></tr>',
        '<tr><th>This Revision:</th><td>{revision}</td></tr>',
        '<tr><th>Revision Comment:</th><td>{revisionLog}</td></tr>',
        '</table>'
    );
    this.seriesDescTpl.dateFmt = function(value) {
        return Ext.util.Format.date(
                new Date(value), 'd M Y, g:i:s a');
    }
    this.seriesDescTpl.compile();

    //
    // VIEWPORT
    //
    new Ext.Viewport({
        layout: 'fit',
        items: [{
            title: 'Virtual Rock Laboratory',
            iconCls: 'auscope-icon',
            layout: 'fit',
            items: [{
                id: 'main-tabpanel',
                xtype: 'grouptabpanel',
                activeGroup: 0,
                tabWidth: 130,
                items: [{
                    mainItem: 0,
                    defaults: { style: 'padding:10px' },
                    items: [{
                        title: 'Series',
                        layout: 'ux.center',
                        bodyStyle: 'padding: 50px 0',
                        items: [{
                            title: 'Series Summary',
                            width: 500,
                            autoHeight: true,
                            layout: 'fit',
                            items: [{
                                id: 'seriesdesc-panel',
                                height: 300,
                                border: false,
                                bodyStyle: 'text-align:left;padding:5px'
                            }],
                            buttonAlign: 'center',
                            buttons: [{
                                text: 'Create Copy...',
                                height: 30,
                                width: 100,
                                iconCls: 'add-icon',
                                handler: this.cloneSeries
                            },{
                                text: 'Close',
                                height: 30,
                                width: 100,
                                iconCls: 'cross-icon',
                                handler: this.closeSeries
                            }]
                        }]
                    },{
                        id: 'jobs-tab',
                        title: 'Job Manager',
                        iconCls: 'grid-icon',
                        layout: 'fit',
                        items: this.JobManager.create()
                    },{
                        id: 'files-tab',
                        title: 'File Manager',
                        iconCls: 'folder-icon',
                        layout: 'fit',
                        items: this.FileManager.create()
                    },{
                        id: 'editor-tab',
                        title: 'Editor',
                        iconCls: 'edit-icon',
                        xtype: 'tabpanel'
                    }]
                }]
            }]
        }]
    });
    Ext.getCmp('main-tabpanel').hide();

    // avoid accidentally navigating away from this page when a series is open
    Ext.EventManager.on(window, 'beforeunload',
            this.onWindowUnloading, this);

    if (!Ext.isEmpty(this.error)) {
        this.showMessage(this.error, 'e', this.showWelcome, this);
        this.error = undefined;
    } else {
        this.showWelcome.call(this);
        this.hideProgressDlg();
    }
}

});

Ext.onReady(VRL.initialize, VRL);

