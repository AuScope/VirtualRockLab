
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"LinearVisc.json"});

LinearViscNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    LinearViscNode.superclass.constructor.apply(this,
        [container, "Linear Viscosity", "LinearVisc", "i"]
    );
    var numInts = container.getInteractions().length;
    this.values.uniqueName = "interaction"+numInts;
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   LinDampingPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      viscosity = "+this.values.viscosity+",\n";
    ret+="      maxIterations = "+this.values.maxIter+"\n   )\n)\n\n";
    return ret;
  }
});

