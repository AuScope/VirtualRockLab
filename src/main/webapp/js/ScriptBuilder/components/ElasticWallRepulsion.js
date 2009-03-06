
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"ElasticWallRepulsion.json"});

ElasticWallRepulsionNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    ElasticWallRepulsionNode.superclass.constructor.apply(this,
        [container, "Elastic Repulsion", "ElasticWallRepulsion", "i"]
    );
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   NRotElasticWallPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      wallName = \""+this.values.wallName+"\",\n";
    ret+="      normalK = "+this.values.normalK+"\n   )\n)\n\n";
    return ret;
  }
});

