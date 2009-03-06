
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"BondedWall.json"});

BondedWallNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    BondedWallNode.superclass.constructor.apply(this,
        [container, "Bonded Elastic", "BondedWall", "i"]
    );
  },

  getScript: function() {
    var ret="sim.createInteractionGroup (\n   NRotBondedWallPrms (\n";
    ret+="      name = \""+this.values.uniqueName+"\",\n";
    ret+="      wallName = \""+this.values.wallName+"\",\n";
    ret+="      normalK = "+this.values.normalK+",\n";
    ret+="      particleTag = "+this.values.tag+"\n   )\n)\n\n";

    return ret;
  }
});

