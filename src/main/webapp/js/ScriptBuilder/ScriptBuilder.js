/*
 * ESyS Particle Script Builder Web Interface
 * (c) 2009 ESSCC, The University of Queensland, Australia
 * All rights reserved.
 */

// reference local blank image
Ext.BLANK_IMAGE_URL = 'js/ext/resources/images/default/s.gif';

Ext.namespace('ScriptBuilder');

//
// Extend the XmlTreeLoader to set some custom TreeNode attributes specific to
// our application:
//
ESySComponentLoader = Ext.extend(Ext.ux.XmlTreeLoader, {
    processAttributes : function(attr) {
        if (attr.name) { // category node
            
            // Set the node text that will show in the tree
            attr.text = attr.name;
            
            // Choose icon using the type flag
            //attr.iconCls = 'icon-' + attr.type;
            
            // Override these values for our folder nodes because we are
            // loading all data at once. If we were loading each node
            // asynchronously (the default) we would not want to do this:
            attr.loaded = true;
        } else if (attr.title) { // component node
            
            attr.text = attr.title;
            //attr.iconCls = 'book';
            attr.leaf = true;
        }
    }
});

// opens the configuration dialog for given component type and ensures
// that values are stored and restored accordingly
ScriptBuilder.showDialog = function(compId, compTitle, existingNode) {
    var dlgContents = Ext.getCmp(compId+'Form');
    var rootNode = Ext.getCmp('usedcomps-panel').getRootNode();

    // Fill the form elements with correct values if editing component
    if (existingNode) {
        if (compId == "SimContainer") {
            rootNode.fillFormValues(dlgContents.getForm());
        } else {
            existingNode.fillFormValues(dlgContents.getForm());
        }
    }
    var dlg = new Ext.Window({
        title: compTitle+' Settings',
        plain: true,
        minWidth: 300,
        minHeight: 200,
        width: 500,
        resizable: false,
        autoScroll: true,
        constrainHeader: true,
        bodyStyle:'padding:5px;',
        items: dlgContents,
        modal: true,
        buttons: [{
            text: 'OK',
            handler: function() {
                if (!dlgContents.getForm().isValid()) {
                    Ext.Msg.alert('Invalid Field(s)', 'Please provide values for all fields!');
                    return;
                }
                if (!existingNode) {
                    var newnode = new dlgContents.nclass(rootNode);
                    if (!newnode.setValues(dlgContents.getForm())) {
                        return;
                    }
                    rootNode.addComponent(newnode);
                } else {
                    var res;
                    if (compId == "SimContainer") {
                        res = rootNode.setValues(dlgContents.getForm());
                    } else {
                        res = existingNode.setValues(dlgContents.getForm());
                    }
                    if (res != true) {
                        return;
                    }
                }
                ScriptBuilder.updateSource();
                dlg.close();
            }
        }, {
            text: 'Cancel',
            handler: function() { dlg.close(); }
        }]
    });
    // Prevent destruction of dlgContents (IE needs this)
    dlg.on('beforedestroy', function() { return false; });
    dlg.show();
}

// updates the source textarea with the current script text
ScriptBuilder.updateSource = function() {
    var s = Ext.getCmp('usedcomps-panel').getRootNode().getScript();
    Ext.getCmp('sourcetext').setValue(s);
}

//
// This is the main layout definition.
//
ScriptBuilder.initialize = function() {
    
    Ext.QuickTips.init();
    
    // a template for the component description html area
    var descTpl = new Ext.Template(
        '<h2 class="title">{title}</h2>',
        '<p>{innerText}</p>'
    );
    descTpl.compile();
    var descText = '<p class="desc-info">Select a component to see its description, double-click to add it to the script.<br/><br/>Double-click the Simulation Container to change simulation settings.</p>';

    // the tree that holds added components
    var usedCompsTree = new Ext.tree.TreePanel({
        xtype: 'treepanel',
        id: 'usedcomps-panel',
        title: 'Used Components',
        collapsible: true,
        region: 'west',
        floatable: false,
        margins: '5 0 0 0',
        cmargins: '5 5 0 0',
        width: 175,
        minSize: 100,
        maxSize: 250,
        rootVisible: true,
        root: new SimContainerNode,
        //rootVisible: false,
        //root: new Ext.tree.TreeNode(),
        contextMenu: new Ext.menu.Menu({
            items: [{
                id: 'delete-node',
                text: 'Delete'
            }],
            listeners: {
                itemclick: function(item) {
                    switch (item.id) {
                        case 'delete-node':
                            var n = item.parentMenu.contextNode;
                            if (n.parentNode.canRemove(n)) {
                                Ext.Msg.confirm('Delete Node', 'Are you sure you want to delete the selected component?',
                                    function(btn) {
                                        if (btn=='yes') {
                                            n.parentNode.removeComponent(n);
                                            ScriptBuilder.updateSource();
                                        }
                                    }
                                );
                            }
                            break;
                    }
                }
            }
        })
    });

    usedCompsTree.on({
        'beforecollapsenode': function(node, deep, anim) {
            // Fix for the problem that child nodes are not visible if
            // removed and re-added.
            if (node.hasChildNodes()) {
                return false;
            } else {
                return true;
            }
        },

        'contextmenu': function(node, e) {
            // do not show menu for root node
            if (node.parentNode) {
                node.select();
                var c = node.getOwnerTree().contextMenu;
                c.contextNode = node;
                c.showAt(e.getXY());
            }
        },

        'dblclick': function(node) {
            ScriptBuilder.showDialog(node.compId, node.text, node);
        }
    });

    // the source textarea embedded in a form for further processing
    // using buttons
    var sourceForm = new Ext.form.FormPanel({
        id: 'source-panel',
        layout: 'fit',
        title: 'Script Source',
        url: 'scriptdownload.jsp',
        standardSubmit: true,
        region:'center',
        buttonAlign: 'right',
        margins: '5 0 0 0',
        buttons: [{
            text: 'Download Script',
            handler: function() {
                // submit script source for download
                Ext.getCmp('scriptaction').setRawValue('download');
                var name = Ext.getCmp('usedcomps-panel').getRootNode().getScriptName();
                Ext.getCmp('scriptname').setRawValue(name);
                Ext.getCmp('source-panel').getForm().submit();
            }
        }, {
            text: 'Use Script',
            handler: function() {
                // submit script source for use in grid submit
                Ext.getCmp('scriptaction').setRawValue('use');
                var name = Ext.getCmp('usedcomps-panel').getRootNode().getScriptName();
                Ext.getCmp('scriptname').setRawValue(name);
                Ext.getCmp('source-panel').getForm().submit({ url: 'scriptbuilder.html' });
            }
        }],
        items: [{
            id: 'sourcetext',
            xtype: 'textarea',
            width: '100%',
            height: '100%',
            readOnly: true
        }, {
            id: 'scriptaction',
            xtype: 'hidden',
            value: 'download'
        }, {
            id: 'scriptname',
            xtype: 'hidden',
            value: 'particle_script'
        }]
    });

    // the tree of available components
    var treePanel = new Ext.tree.TreePanel({
        id: 'tree-panel',
        title: 'Available Components',
        region:'center',
        height: 300,
        minSize: 150,
        autoScroll: true,
        
        // tree-specific configs:
        rootVisible: false,
        root: new Ext.tree.AsyncTreeNode(),
        loader: new ESySComponentLoader({
            dataUrl:'js/ScriptBuilder/components.xml'
        })
    });

    // Show corresponding description on click on a component
    treePanel.on({
        'click': function(node) {
            if (node.leaf) { // click on a component
                var descEl = Ext.getCmp('description-panel').body;
                descEl.update('').setStyle('background','#fff');
                descTpl.overwrite(descEl, node.attributes);
            } else { // click on a category
                var descEl = Ext.getCmp('description-panel').body;
                descEl.update(descText).setStyle('background','#eee');
            }
        },
        'dblclick': function(node) {
            if (node.leaf &&
                Ext.getCmp('usedcomps-panel').getRootNode().canAppend(node.attributes)) {
                ScriptBuilder.showDialog(node.attributes.id, node.attributes.title);
            }
        }
    });
    
    // This is the description panel that contains the description for the
    // selected component.
    var descriptionPanel = {
        id: 'description-panel',
        title: 'Component Description',
        region: 'south',
        split: true,
        bodyStyle: 'padding-bottom:15px;background:#eee;',
        autoScroll: true,
        collapsible: true,
        html: descText
    };
    
    // Finally, build the main layout once all the pieces are ready.
    new Ext.Viewport({
        layout: 'border',
        defaults: { layout: 'border' },
        items: [{
            xtype: 'box',
            region: 'north',
            applyTo: 'header',
            height: 30
        },{
            id: 'component-browser',
            region:'west',
            border: false,
            split:true,
            margins: '2 0 5 5',
            width: 250,
            minSize: 150,
            maxSize: 500,
            items: [ treePanel, descriptionPanel ]
        },{
            id: 'content-panel',
            title: 'Current Script',
            region: 'center',
            margins: '2 5 5 0',
            defaults: {
                collapsible: false,
                split: true
            },
            items: [ usedCompsTree, sourceForm ]
        }]
    });

    ScriptBuilder.updateSource();

    // Show settings dialog for the new script
    ScriptBuilder.showDialog('SimContainer', 'New Script',
            Ext.getCmp('usedcomps-panel').getRootNode());
}


Ext.onReady(ScriptBuilder.initialize);

