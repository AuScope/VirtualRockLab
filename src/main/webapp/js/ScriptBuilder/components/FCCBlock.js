
Ext.namespace("ScriptBuilder");
Ext.ux.ComponentLoader.load({url: ScriptBuilder.componentPath+"FCCBlock.json"});

FCCBlockNode = Ext.extend(ScriptBuilder.BaseComponent, {
  constructor: function(container) {
    FCCBlockNode.superclass.constructor.apply(this,
        [container, "FCC Block", "FCCBlock", "g"]
    );
  },

  getUniqueName: function() {
    return "geometry";
  },

  getScript: function() {
    var ret = "geometry = CubicBlock (\n   dimCount = [";
    ret+=this.values.dirX+", "+this.values.dirY+", "+this.values.dirZ+"],\n";
    ret+="   radius = "+this.values.radius+"\n)\n";
    ret+="geometry.rotate (\n   axis = Vec3(";
    ret+=this.values.rotAngleX+", "+this.values.rotAngleY+", "+this.values.rotAngleZ+"),\n";
    ret+="   axisPt = Vec3(";
    ret+=this.values.rotPointX+", "+this.values.rotPointY+", "+this.values.rotPointZ+")\n)\n";
    ret+="geometry.translate ( translation = Vec3(";
    ret+=this.values.transX+", "+this.values.transY+", "+this.values.transZ+") )\n";
    ret+="\nsim.setSpatialDomain ( geometry.getBBox() )\n";
    ret+="sim.createParticles ( geometry )\n\n";

    return ret;
  }
});

