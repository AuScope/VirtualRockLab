
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"ReadGeoFile.json"});

ReadGeoFileNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    ReadGeoFileNode.superclass.constructor.apply(this,
        [container, "Read GEO File", "ReadGeoFile", "g"]
    );
  },

  getUniqueName: function() {
    return "geometry";
  },

  getScript: function() {
    var ret="sim.readGeometry ( fileName = \""+this.values.fileName+"\" )\n";
    return ret;
  }
});

