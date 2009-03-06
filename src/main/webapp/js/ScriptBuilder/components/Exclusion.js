
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"Exclusion.json"});

ExclusionNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    ExclusionNode.superclass.constructor.apply(this,
        [container, "Interaction Exclusion Principle", "Exclusion", "xx"]
    );
  },

  getUniqueName: function() {
    return "pp_exclusion";
  },

  getScript: function() {
    var ret="sim.createExclusion (\n";
    ret+="   interactionName1 = \""+this.values.intName1+"\",\n";
    ret+="   interactionName2 = \""+this.values.intName2+"\"\n)\n\n";

    return ret;
  }
});

