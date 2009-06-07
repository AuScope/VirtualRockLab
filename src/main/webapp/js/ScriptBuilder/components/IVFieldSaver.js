
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"IVFieldSaver.json"});

IVFieldSaverNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    IVFieldSaverNode.superclass.constructor.apply(this,
        [container, "Interaction Vector Field Saver", "IVFieldSaver", "f"]
    );
  },

  fillFormValues: function(form) {
    form.setValues(this.values);
    var intList = this.container.getInteractions();
    var store = form.findField('intName').getStore();
    store.removeAll();
    for (var i=0; i<intList.length; i++) {
        store.add(new store.recordType({'text': intList[i].getUniqueName()}));
    }
  },

  getScript: function() {
    var ret = this.values.uniqueName+" = InteractionVectorFieldSaverPrms (\n";
    ret+="   interactionName = \""+this.values.intName+"\",\n";
    ret+="   fieldName = \""+this.values.fieldName+"\",\n";
    ret+="   fileName = \""+this.values.fileName+"\",\n";
    ret+="   fileFormat = \""+this.values.fileFormat+"\",\n";
    ret+="   beginTimeStep = "+this.values.beginTS+",\n";
    ret+="   endTimeStep = "+this.values.endTS+",\n";
    ret+="   timeStepIncr = "+this.values.timeIncrement+"\n)\n";
    ret+="sim.createFieldSaver ( "+this.values.uniqueName+" )\n\n";
    return ret;
  }
});

