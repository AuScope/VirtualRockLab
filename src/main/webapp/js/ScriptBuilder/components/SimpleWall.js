
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"SimpleWall.json"});

SimpleWallNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    SimpleWallNode.superclass.constructor.apply(this,
        [container, "Simple Wall", "SimpleWall", "w"]
    );
  },

  getScript: function() {
    var ret="sim.createWall (\n   name = \""+this.values.uniqueName+"\",\n";
    ret+="   posn = Vec3("+this.values.originX+", "+this.values.originY+", "+this.values.originZ+"),\n";
    ret+="   normal = Vec3("+this.values.normalX+", "+this.values.normalY+", "+this.values.normalZ+")\n)\n\n";
    return ret;
  }
});

