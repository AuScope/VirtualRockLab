<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>

<head>
    <title>AuScope Virtual Rock Laboratory - MyProxy Login</title>
    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      .error { font-weight: bold; color: red; }
    </style>
</head>

<body>
<%@ include file="page_header.jsp" %>
    <div id="body" style="padding:10px;">
        <p style="font-size:medium;padding-bottom:5px;">
<% if (request.getHeader("Shib-AuEduPerson-SharedToken") == null) { %>
        Your institution (IdP) does not support the &quot;shared token&quot;
        attribute. This means that you will need your own certificate to access
        the grid. If you have a valid grid certificate please upload it to the
        ARCS MyProxy server (e.g. using the
        <a href="http://www.arcs.org.au/products-services/authorisation-services/grix">grix</a> tool).
        <br/>
<% } %>
        Please enter your MyProxy username and password.<br/>
        These details will be used to authenticate yourself to the grid.
    </p>

    <form method="post" name="loginForm">
        <table width="250px" bgcolor="f8f8ff" border="0" cellspacing="0" cellpadding="5">
        <tr>
            <td align="right" width="50%">Username:</td>
            <td width="50%"><input type="text" name="username"/></td>
        </tr>
        <tr>
            <td align="right" width="50%">Password:</td>
            <td width="50%"><input type="password" name="password"/></td>
        </tr>
        <tr>
            <td colspan="2" align="right">
                <input type="submit" value="Login"/>
            </td>
        </tr>
        </table>
    </form>
<c:if test='${error != null}'>
    <p class="error">${error}
    <br>
    Please ensure that you have a valid grid certificate and uploaded it to the
    ARCS MyProxy server (for example using 'grix').
    </p>
</c:if>
    </div>
</body>

</html>

