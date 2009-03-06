
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"Gravity.json"});

GravityNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    GravityNode.superclass.constructor.apply(this,
        [container, "Gravity", "Gravity", "i"]
    );
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   GravityPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      acceleration = Vec3("+this.values.accX+", "+this.values.accY+", "+this.values.accZ+")\n";
    ret+="   )\n)\n\n";
    return ret;
  }
});

