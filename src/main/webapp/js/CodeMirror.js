Ext.namespace('Ext.ux.form');

Ext.ux.form.CodeMirror = Ext.extend(Ext.form.TextArea, {
    codeMirrorPath: 'js/codemirror',
    language: 'python',
    readOnly: false,
    initComponent: function() {
        if (this.codeMirrorPath === null) {
            throw 'Ext.ux.form.CodeMirror: codeMirrorPath required';
        }
        this.initialized = false;
        Ext.ux.form.CodeMirror.superclass.initComponent.apply(this, arguments);
        this.addEvents('initialize');
        this.on({
            resize: function(ta, width, height, rawW, rawH) {
                width -= 40; // for the line numbers
                var el = Ext.select('.'+this.id);
                if (el.elements.length > 0) {
                    el.setSize(width, height);
                } else {
                    ta.initWidth=width;
                    ta.initHeight=height;
                }
            },
            afterrender: function() {
                var me = this;
                var parser;
                var stylesheet = this.codeMirrorPath+'/pythoncolors.css';
                if (this.language.toLowerCase() === 'python') {
                    parser='parsepython.js';
                } else {
                    parser='parsedummy.js';
                }

                            (function() {
                me.codeEditor = new CodeMirror.fromTextArea(me.id, {
                    parserfile: parser,
                    stylesheet: stylesheet,
                    parserConfig: {'pythonVersion':2, 'strictErrors':true},
                    path: me.codeMirrorPath+'/',
                    textWrapping: false,
                    indentUnit: 4,
                    lineNumbers: true,
                    iframeClass: 'codemirror-iframe '+me.id,
                    content: me.value,
                    readOnly: me.readOnly,
                    width: me.initWidth+"px",
                    height: me.initHeight+"px",
                    initCallback: function() {
                        me.initialized = true;
                        me.fireEvent('initialize', true);
                        me.initWidth = undefined;
                        me.initHeight = undefined;
                    }
                });
                            }).defer(100);                
            }
        });
    },
    getValue: function() {
        if (this.initialized) {
            return this.codeEditor.getCode();
        }
        return this.value;    
    },
    setValue: function(v) {
        if (this.initialized) {
            this.codeEditor.setCode(v);
        }
    },
    validate: function() {
        this.getValue();
        Ext.ux.form.CodeMirror.superclass.validate.apply(this, arguments);
    }
});
Ext.reg('ux-codemirror', Ext.ux.form.CodeMirror);

