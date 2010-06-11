<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>

<head>
    <title>Virtual Rock Laboratory - MyProxy Login</title>
    <link rel="stylesheet" type="text/css" href="css/vrl.min.css">
    <style type="text/css">
      .error { font-size:medium;font-weight:bold;color:red; }
      .note { font-size:medium;font-weight:bold;color:blue; }
    </style>
    <script type="text/javascript">
        function doSubmit() {
            document.getElementById("subBtn").disabled=true;
            document.getElementById("noteMsg").style.visibility="visible";
        }
    </script>
</head>

<body>
<%@ include file="page_header.jsp" %>
    <div id="body" style="padding:10px;">
        <p style="font-size:medium;padding-bottom:5px;">
<% if (request.getHeader("Shib-AuEduPerson-SharedToken") == null) {%>
<%     if (request.getHeader("Shib-Person-commonName") != null) {%>
        Hello <%=request.getHeader("Shib-Person-commonName")%>,<br/>
<%     }%>
        Your institution (IdP) does not support the &quot;shared token&quot;
        attribute. This means that you will need your own certificate to access
        Grid resources.<br/>
<% }%>
        If you have a valid Grid certificate please upload it to the ARCS
        MyProxy server (e.g. using the <a href="http://www.arcs.org.au/index.php/services/services-list/290-grix">grix tool</a>)
        then use the fields below to to enter the MyProxy username and
        password you used.
    </p>

    <form method="post" name="loginForm">
        <table width="250px" bgcolor="f8f8ff" border="0" cellspacing="0" cellpadding="5">
        <tr>
            <td align="right" width="50%">Username:</td>
            <td width="50%"><input type="text" name="proxyuser"/></td>
        </tr>
        <tr>
            <td align="right" width="50%">Password:</td>
            <td width="50%"><input type="password" name="proxypass"/></td>
        </tr>
        <tr>
            <td colspan="2" align="right">
                <input id="subBtn" type="submit" value="Login" onclick="doSubmit()"/>
            </td>
        </tr>
        </table>
    </form>
    <p class="note" id="noteMsg" style="visibility:hidden">
        Please stand by while the system retrieves your Grid credentials...
    </p>
<c:if test='${error != null}'>
    <p class="error">${error}
    <br>
    Please double-check that your Grid certificate and proxy is valid for at
    least 24 hours and the entered details are correct.
    </p>
</c:if>
    </div>
</body>

</html>

