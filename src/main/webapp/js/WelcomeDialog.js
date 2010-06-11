/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.WelcomeDialog');

VRL.WelcomeDialog = {

show: function(callback) {
    var w = new Ext.Window({
        layout: 'vbox',
        layoutConfig: { align: 'stretchmax' },
        closable: false,
        resizable: false,
        draggable: false,
        modal: true,
        border: false,
        buttonAlign: 'center',
        width: 450,
        height: 300,
        initHidden: false,
        items: [{
            xtype: 'box',
            height: 85,
            autoEl: {
                tag: 'img',
                src: 'img/img-auscope-banner.gif'
            },
            width: '100%'
        }, {
            xtype: 'label',
            cls: 'welcome-title',
            text: 'Welcome to the Virtual Rock Laboratory!'
        }, {
            xtype: 'label',
            cls: 'welcome-text',
            text: 'Please choose one of the following options to begin:'
        }, {
            xtype: 'button',
            iconCls: 'newseries-icon',
            flex: 1,
            scale: 'large',
            text: 'Create New Series',
            tooltip: 'Creates an empty new series.',
            handler: function() {
                w.on({'close': callback.createCallback(1)});
                w.close();
            }
        }, {
            xtype: 'button',
            iconCls: 'myseries-icon',
            flex: 1,
            scale: 'large',
            text: 'Browse My Series',
            tooltip: 'Use this option if you want to monitor running jobs or use an existing series as a template for a new one.',
            handler: function() {
                w.on({'close': callback.createCallback(2)});
                w.close();
            }
        }, {
            xtype: 'button',
            iconCls: 'examples-icon',
            flex: 1,
            scale: 'large',
            text: 'Browse All Examples',
            tooltip: 'Allows you to browse examples of VRL series and use them to create a new series.',
            handler: function() {
                w.on({'close': callback.createCallback(3)});
                w.close();
            }
        }]
    });
}

}

