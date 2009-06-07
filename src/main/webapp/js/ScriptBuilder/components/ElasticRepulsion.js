
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"ElasticRepulsion.json"});

ElasticRepulsionNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    ElasticRepulsionNode.superclass.constructor.apply(this,
        [container, "Elastic Repulsion", "ElasticRepulsion", "i"]
    );
    var numInts = container.getInteractions().length;
    this.values.uniqueName = "interaction"+numInts;
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   NRotElasticPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      normalK = "+this.values.normalK+"\n   )\n)\n\n";
    return ret;
  }
});

