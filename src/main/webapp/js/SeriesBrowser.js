/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2010 The University of Queensland, ESSCC
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */

Ext.namespace('VRL.SeriesBrowser');

VRL.SeriesBrowser = {

// a template for the description html area
descTpl: new Ext.Template(
    '<p class="jobdesc-title">{name}</p>',
    '<p class="jobdesc-key">Description:</p><br/><p>{description}</p>',
    {
        compiled: true,
        disableFormats: true
    }
),

updateDetails: function(node) {
    if (node.leaf) {
        node.attributes.name = node.parentNode.attributes.name
            +' (Revision '+node.attributes.revision+')';
    }
    var descEl = Ext.getCmp('sb-details-panel').body;
    this.descTpl.overwrite(descEl, node.attributes);
},

show: function(callback, doExamples) {

    var detailsPanel = new Ext.Panel({
        id: 'sb-details-panel',
        title: 'Details',
        bodyStyle: 'padding:10px',
        region: 'west',
        width: '220',
        collapsible: false,
        split: true
    });

    function dateRenderer(value, cell, record) {
        return Ext.util.Format.date(new Date(value), 'd M Y, g:i:s a');
    }

    function dateSorter(node) {
        return parseInt(node.attributes.date, 10);
    }

    var seriesTree = new Ext.ux.tree.TreeGrid({
        title: 'Series',
        id: 'sb-tree',
        region: 'center',
        split: true,
        enableSort: true,
        singleExpand: true,
        columns: [
            { header: 'Name/Revision', width: 160, dataIndex: 'name' },
            { header: 'Date Created', width: 160, dataIndex: 'date',
              sortType: dateSorter, renderer: dateRenderer }
        ],
        loader: new Ext.tree.TreeLoader({
            baseParams: {
                'action': 'listSeries',
                'demo': doExamples || false
            },
            dataUrl: VRL.controllerURL
        }),
        root: new Ext.tree.AsyncTreeNode({id:'series-root'}),
        listeners: {
            scope: this,
            'click': function(node, e) {
                if (!node.isSelected()) {
                    node.select();
                    Ext.getCmp('sb-open-btn').enable();
                    this.updateDetails(node);
                }
            },
            'load': function(loader, node, response) {
                seriesTree.treeLoadMask.hide();
            },
            'loadexception': function(loader, node, response) {
                seriesTree.treeLoadMask.hide();
                VRL.onLoadException(
                        loader, 'remote', null, null, response);
            }
        }
    });

    seriesTree.getLoader().on('beforeload',
        function(loader, node, callback) {
            if (!seriesTree.treeLoadMask) {
                seriesTree.treeLoadMask = new Ext.LoadMask('sb-tree', {
                    msg: 'Loading data, please wait...'
                });
            }
            seriesTree.treeLoadMask.show();
        },
        this,
        { delay: 1 }
    );

    var w = new Ext.Window({
        title: 'VRL Series Browser',
        modal: true,
        layout: 'border',
        closable: false,
        constrain: true,
        constrainHeader: true,
        initHidden: false,
        width: 600,
        height: 400,
        buttons: [{
            text: 'Cancel',
            handler: function() {
                w.on({'close': callback.createCallback()});
                w.close();
            }
        }, {
            text: 'Open',
            id: 'sb-open-btn',
            disabled: true,
            handler: function() {
                w.on({'close': callback.createCallback(
                    seriesTree.getSelectionModel().
                        getSelectedNode().attributes)});
                w.close();
            }
        }],
        items: [
            detailsPanel,
            seriesTree
        ]
    });
}

}

