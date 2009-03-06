
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"WallLoader.json"});

WallLoaderNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    WallLoaderNode.superclass.constructor.apply(this,
        [container, "Wall Loader", "WallLoader", "r"]
    );
  },

  getScript: function() {
    var ret = "from WallLoader import WallLoaderRunnable\n\n";
    ret+=this.values.uniqueName+" = WallLoaderRunnable (\n";
    ret+="   LsmMpi = sim,\n   wallName = "+this.values.wallName+",\n";
    ret+="   vPlate = Vec3("+this.values.plateX+", "+this.values.plateY+", "+this.values.plateZ+"),\n";
    ret+="   dt = "+this.container.getValues().timeIncrement+"\n)\n";
    if (this.values.snapTiming == 0) {
      ret+="sim.addPreTimeStepRunnable ( "+this.values.uniqueName+" )\n\n";
    } else {
      ret+="sim.addPostTimeStepRunnable ( "+this.values.uniqueName+" )\n\n";
    }
    return ret;
  }
});

