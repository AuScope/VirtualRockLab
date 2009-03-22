<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="include.jsp" %>

<html>

<head>
    <title>ESyS-Particle Script Builder</title>

    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      #sitenav-02 a {
        background: url( "img/navigation.gif" ) -100px -38px no-repeat;
      }
    </style>
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/ext-all.css">
    <script type="text/javascript" src="js/ext/adapter/ext/ext-base.js"></script>
    <script type="text/javascript" src="js/ext/ext-all.js"></script>
    <script type="text/javascript" src="js/ScriptBuilder/ComponentLoader.js"></script>
    <script type="text/javascript" src="js/ScriptBuilder/XmlTreeLoader.js"></script>

    <!-- component includes -->
    <%
    String[] comps = { "BaseComponent", "SimContainer", "SimpleBlock",
        "FCCBlock", "HCPBlock", "RandomBlock", "ReadGeoFile", "SimpleWall",
        "ElasticRepulsion", "BondedElastic", "BondedRotElastic", "Friction",
        "RotFriction", "Exclusion", "ElasticWallRepulsion", "BondedWall",
        "Gravity", "LinearVisc", "RotationalVisc", "PSFieldSaver",
        "PVFieldSaver", "ISFieldSaver", "IVFieldSaver", "WVFieldSaver",
        "CheckPointer", "IVRunnable", "WallLoader" };
    for (String c : comps) {
    %>
    <script type="text/javascript" src="js/ScriptBuilder/components/<%= c %>.js"></script>
    <%
    }
    // ScriptBuilder.js must come last!
    %>
    <script type="text/javascript" src="js/ScriptBuilder/ScriptBuilder.js"></script>
</head>

<body>
    <%@ include file="page_header.jsp" %>
    <div id="body"></div>
</body>

</html>

