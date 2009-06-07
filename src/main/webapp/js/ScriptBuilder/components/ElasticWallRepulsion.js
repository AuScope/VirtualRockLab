
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"ElasticWallRepulsion.json"});

ElasticWallRepulsionNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    ElasticWallRepulsionNode.superclass.constructor.apply(this,
        [container, "Elastic Repulsion", "ElasticWallRepulsion", "i"]
    );
    var numInts = container.getInteractions().length;
    this.values.uniqueName = "interaction"+numInts;
  },

  fillFormValues: function(form) {
    form.setValues(this.values);
    var wallList = this.container.getWalls();
    var store = form.findField('wallName').getStore();
    store.removeAll();
    for (var i=0; i<wallList.length; i++) {
        store.add(new store.recordType({'text': wallList[i].getUniqueName()}));
    }
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   NRotElasticWallPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      wallName = \""+this.values.wallName+"\",\n";
    ret+="      normalK = "+this.values.normalK+"\n   )\n)\n\n";
    return ret;
  }
});

