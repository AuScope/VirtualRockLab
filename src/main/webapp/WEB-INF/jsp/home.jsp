<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="include.jsp" %>

<html>

<head>
    <title>Virtual Rock Laboratory</title>
    <link rel="stylesheet" type="text/css" href="css/vrl.min.css">
    <style type="text/css">
      .CodeMirror-line-numbers {
        width: 2.2em;
        color: #aaa;
        background-color: #eee;
        text-align: right;
        padding: .4em;
        margin: 0;
        font-family: monospace;
        font-size: 10pt;
        line-height: 1.1em;
      }
    </style>

    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/ext-all.css">
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/GroupTab.css">
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/CenterLayout.css">
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/fileuploadfield.css">

    <script type="text/javascript" src="js/ext/adapter/ext/ext-base-debug.js"></script>
    <script type="text/javascript" src="js/ext/ext-all-debug.js"></script>

    <script type="text/javascript" src="js/vrl.js"></script>
    <c:if test='${error != null}'>
    <script type="text/javascript">
        VRL.error = "${error}";
    </script>
    </c:if>
</head>

<body>
    <div id="body"></div>
</body>

</html>

