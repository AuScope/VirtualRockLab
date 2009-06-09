<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="include.jsp" %>

<html>

<head>
    <title>AuScope Virtual Rock Laboratory - Home</title>
    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      #sitenav-01 a {
        background: url( "img/navigation.gif" ) 0px -38px no-repeat;
      }
    </style>
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/ext-all.css">
    <script type="text/javascript" src="js/ext/adapter/ext/ext-base.js"></script>
    <script type="text/javascript" src="js/ext/ext-all.js"></script>
    <script type="text/javascript" src="js/JobList.js"></script>
    <c:if test='${error != null}'>
    <script type="text/javascript">
        JobList.error = "${error}";
    </script>
    </c:if>
</head>

<body>
    <%@ include file="page_header.jsp" %>
    <div id="body"></div>
</body>

</html>

