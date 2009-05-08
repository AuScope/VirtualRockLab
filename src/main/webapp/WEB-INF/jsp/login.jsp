<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>

<head>
    <title><fmt:message key="title"/></title>
    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      #sitenav-03 a {
        background: url( "img/navigation.gif" ) -200px -38px no-repeat;
      }
      .error { color: red; }
    </style>
</head>

<body>
    <%@ include file="page_header.jsp" %>
    <div id="body"></div>

    <h1>Login</h1>
    <br>

    <form method="post" name="loginForm">
        <table width="25%" bgcolor="f8f8ff" border="0" cellspacing="0" cellpadding="5">
        <tr>
            <td align="right" width="50%">Username:</td>
            <td width="50%"><input type="text" name="username"/></td>
        </tr>
        <tr>
            <td align="right" width="50%">Password:</td>
            <td width="50%"><input type="password" name="password"/></td>
        </tr>
        </table>
        <br>
        <input type="submit" align="center" value="Submit"/>
    </form>
</body>

</html>

