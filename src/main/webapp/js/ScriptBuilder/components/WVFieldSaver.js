
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"WVFieldSaver.json"});

WVFieldSaverNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    WVFieldSaverNode.superclass.constructor.apply(this,
        [container, "Wall Vector Field Saver", "WVFieldSaver", "f"]
    );
  },

  getScript: function() {
    var ret = this.values.uniqueName+" = WallVectorFieldSaverPrms (\n";
    ret+="   wallName = \""+this.values.wallName+"\",\n";
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

