/*!
 * Ext JS Library 3.2.1
 * Copyright(c) 2006-2010 Ext JS, Inc.
 * licensing@extjs.com
 * http://www.extjs.com/license
 */
Ext.ux.GroupTab = Ext.extend(Ext.Container, {
    mainItem: 0,
    
    expanded: true,
    
    deferredRender: true,
    
    activeTab: null,
    
    idDelimiter: '__',
    
    headerAsText: false,
    
    frame: false,
    
    hideBorders: true,
    
    initComponent: function(config){
        Ext.apply(this, config);
        this.frame = false;
        
        Ext.ux.GroupTab.superclass.initComponent.call(this);
        
        this.addEvents('activate', 'deactivate', 'changemainitem', 'beforetabchange', 'tabchange');
        
        this.setLayout(new Ext.layout.CardLayout({
            deferredRender: this.deferredRender
        }));
        
        if (!this.stack) {
            this.stack = Ext.TabPanel.AccessStack();
        }
        
        this.initItems();
        
        this.on('beforerender', function(){
            this.groupEl = this.ownerCt.getGroupEl(this);
        }, this);
        
        this.on('add', this.onAdd, this, {
            target: this
        });
        this.on('remove', this.onRemove, this, {
            target: this
        });
        
        if (this.mainItem !== undefined) {
            var item = (typeof this.mainItem == 'object') ? this.mainItem : this.items.get(this.mainItem);
            delete this.mainItem;
            this.setMainItem(item);
        }
    },
    
    /**
     * Sets the specified tab as the active tab. This method fires the {@link #beforetabchange} event which
     * can return false to cancel the tab change.
     * @param {String/Panel} tab The id or tab Panel to activate
     */
    setActiveTab : function(item){
        item = this.getComponent(item);
        if(!item){
            return false;
        }
        if(!this.rendered){
            this.activeTab = item;
            return true;
        }
        if(this.activeTab != item && this.fireEvent('beforetabchange', this, item, this.activeTab) !== false){
            if(this.activeTab && this.activeTab != this.mainItem){
                var oldEl = this.getTabEl(this.activeTab);
                if(oldEl){
                    Ext.fly(oldEl).removeClass('x-grouptabs-strip-active');
                }
            }
            var el = this.getTabEl(item);
            Ext.fly(el).addClass('x-grouptabs-strip-active');
            this.activeTab = item;
            this.stack.add(item);

            this.layout.setActiveItem(item);
            if(this.layoutOnTabChange && item.doLayout){
                item.doLayout();
            }
            if(this.scrolling){
                this.scrollToTab(item, this.animScroll);
            }

            this.fireEvent('tabchange', this, item);
            return true;
        }
        return false;
    },
    
    getTabEl: function(item){
        if (item == this.mainItem) {
            return this.groupEl;
        }
        return Ext.TabPanel.prototype.getTabEl.call(this, item);
    },
    
    onRender: function(ct, position){
        Ext.ux.GroupTab.superclass.onRender.call(this, ct, position);
        
        this.strip = Ext.fly(this.groupEl).createChild({
            tag: 'ul',
            cls: 'x-grouptabs-sub'
        });

        this.tooltip = new Ext.ToolTip({
           target: this.groupEl,
           delegate: 'a.x-grouptabs-text',
           trackMouse: true,
           renderTo: document.body,
           listeners: {
               beforeshow: function(tip) {
                   var item = (tip.triggerElement.parentNode === this.mainItem.tabEl)
                       ? this.mainItem
                       : this.findById(tip.triggerElement.parentNode.id.split(this.idDelimiter)[1]);

                   if(!item.tabTip) {
                       return false;
                   }
                   tip.body.dom.innerHTML = item.tabTip;
               },
               scope: this
           }
        });
                
        if (!this.itemTpl) {
            var tt = new Ext.Template('<li class="{cls}" id="{id}">', '<a onclick="return false;" class="x-grouptabs-text {iconCls}">{text}</a>', '</li>');
            tt.disableFormats = true;
            tt.compile();
            Ext.ux.GroupTab.prototype.itemTpl = tt;
        }
        
        this.items.each(this.initTab, this);
    },
    
    afterRender: function(){
        Ext.ux.GroupTab.superclass.afterRender.call(this);
        
        if (this.activeTab !== undefined) {
            var item = (typeof this.activeTab == 'object') ? this.activeTab : this.items.get(this.activeTab);
            delete this.activeTab;
            this.setActiveTab(item);
        }
    },
    
    // private
    initTab: function(item, index){
        var before = this.strip.dom.childNodes[index];
        var p = Ext.TabPanel.prototype.getTemplateArgs.call(this, item);
        
        if (item === this.mainItem) {
            item.tabEl = this.groupEl;
            p.cls += ' x-grouptabs-main-item';
        }
        
        var el = before ? this.itemTpl.insertBefore(before, p) : this.itemTpl.append(this.strip, p);
        
        item.tabEl = item.tabEl || el;
                
        item.on('disable', this.onItemDisabled, this);
        item.on('enable', this.onItemEnabled, this);
        item.on('titlechange', this.onItemTitleChanged, this);
        item.on('iconchange', this.onItemIconChanged, this);
        item.on('beforeshow', this.onBeforeShowItem, this);
    },
    
    setMainItem: function(item){
        item = this.getComponent(item);
        if (!item || this.fireEvent('changemainitem', this, item, this.mainItem) === false) {
            return;
        }
        
        this.mainItem = item;
    },
    
    getMainItem: function(){
        return this.mainItem || null;
    },
    
    // private
    onBeforeShowItem: function(item){
        if (item != this.activeTab) {
            this.setActiveTab(item);
            return false;
        }
    },
    
    // private
    onAdd: function(gt, item, index){
        if (this.rendered) {
            this.initTab.call(this, item, index);
        }
    },
    
    // private
    onRemove: function(tp, item){
        Ext.destroy(Ext.get(this.getTabEl(item)));
        this.stack.remove(item);
        item.un('disable', this.onItemDisabled, this);
        item.un('enable', this.onItemEnabled, this);
        item.un('titlechange', this.onItemTitleChanged, this);
        item.un('iconchange', this.onItemIconChanged, this);
        item.un('beforeshow', this.onBeforeShowItem, this);
        if (item == this.activeTab) {
            var next = this.stack.next();
            if (next) {
                this.setActiveTab(next);
            }
            else if (this.items.getCount() > 0) {
                this.setActiveTab(0);
            }
            else {
                this.activeTab = null;
            }
        }
    },
    
    // private
    onBeforeAdd: function(item){
        var existing = item.events ? (this.items.containsKey(item.getItemId()) ? item : null) : this.items.get(item);
        if (existing) {
            this.setActiveTab(item);
            return false;
        }
        Ext.TabPanel.superclass.onBeforeAdd.apply(this, arguments);
        var es = item.elements;
        item.elements = es ? es.replace(',header', '') : es;
        item.border = (item.border === true);
    },
    
    // private
    onItemDisabled: Ext.TabPanel.prototype.onItemDisabled,
    onItemEnabled: Ext.TabPanel.prototype.onItemEnabled,
    
    // private
    onItemTitleChanged: function(item){
        var el = this.getTabEl(item);
        if (el) {
            Ext.fly(el).child('a.x-grouptabs-text', true).innerHTML = item.title;
        }
    },
    
    //private
    onItemIconChanged: function(item, iconCls, oldCls){
        var el = this.getTabEl(item);
        if (el) {
            Ext.fly(el).child('a.x-grouptabs-text').replaceClass(oldCls, iconCls);
        }
    },
    
    beforeDestroy: function(){
        Ext.TabPanel.prototype.beforeDestroy.call(this);
        this.tooltip.destroy();
    },

    //add method taken from Ext.Container, modified to call onAdd with the
    //correct arguments
    add: function(comp) {
        this.initItems();
        var args = arguments.length > 1;
        if (args || Ext.isArray(comp)) {
            var result = [];
            Ext.each(args ? arguments: comp,
            function(c) {
                result.push(this.add(c));
            },
            this);
            return result;
        }
        var c = this.lookupComponent(this.applyDefaults(comp));
        var index = this.items.length;
        if (this.fireEvent('beforeadd', this, c, index) !== false && this.onBeforeAdd(c) !== false) {
            this.items.add(c);

            c.onAdded(this, index);
            this.onAdd(this, c, index);
            // this.fireEvent('add', this, c, index);
        }
        return c;
    },

    //hacked version of doRemove from Ext.Container, I'm sure this isn't right but it 'appears' to be working
    doRemove: function(c, autoDestroy) {
        if (this.layout && this.rendered) {
            this.layout.onRemove(c);
        }
        this.items.remove(c);
        c.onRemoved();
        this.onRemove(this,c);
        if (autoDestroy === true || (autoDestroy !== false && this.autoDestroy)) {
            c.destroy();
            Ext.get(c.tabEl).remove();
        }
    }
});

Ext.reg('grouptab', Ext.ux.GroupTab);
