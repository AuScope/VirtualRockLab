<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="org.springframework.security.AuthenticationException"%>
<%@page import="org.springframework.security.DisabledException"%>
<%@page import="org.springframework.security.userdetails.UsernameNotFoundException"%>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>

<head>
    <title>AuScope Virtual Rock Laboratory - Access Denied</title>
    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      .error { font-size: medium; font-weight: bold; color: red; }
    </style>
</head>

<body>
    <div id="header-container">
        <div id="logo">
            <img alt="" src="../img/img-auscope-banner.gif" />
        </div>
    </div>

    <div id="body" style="padding:10px;">
        <p class="error">Access Denied</p>
        <p style="font-size:small">
<%
AuthenticationException e = (AuthenticationException)request.getSession()
    .getAttribute("SPRING_SECURITY_LAST_EXCEPTION");

if (request.getMethod().equalsIgnoreCase("POST")) {
%>
            <p style="font-size:medium">
            Thank you for registering. We will contact you shortly via email!
            </p>
<%
} else if (e instanceof DisabledException) {
%>
            Your portal account has been disabled.<br>
            If you believe this is an error please contact the site
            administrators.
<%
} else if (e instanceof UsernameNotFoundException) {
%>
            The Escript Portal is only accessible to registered users.<br>
            If you would like to register now please use the button.<br>
            Your details will be forwarded to the administrators who will be in
            touch with you via email shortly.<br>
            <form action="access_error.html" method="POST">
                <input type="submit" value="Register" />
            </form>
<%
} else {
%>
            You are not allowed to access this page.<br>
            If you believe this is an error please contact the site
            administrators.
<%
}
%>
        </p>
    </div>
</body>

</html>

