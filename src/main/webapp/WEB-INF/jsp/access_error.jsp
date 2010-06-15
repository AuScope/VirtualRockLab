<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="org.springframework.security.AuthenticationException"%>
<%@page import="org.springframework.security.DisabledException"%>
<%@page import="org.springframework.security.userdetails.UsernameNotFoundException"%>
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>

<head>
    <title>Virtual Rock Laboratory - Access Denied</title>
    <link rel="stylesheet" type="text/css" href="css/vrl.min.css">
    <style type="text/css">
      .error { font-size:medium; font-weight:bold; color:red; }
      .att-table {
        font-size:16px;
        margin-bottom:10px;
      }
      .att-table td, .att-table th {
        padding:5px;
        border:1px solid black;
      }
      .att-table th {
        text-align:right;
        width:120px;
        background-color:#C0C0C0;
      }
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

    if (request.getMethod().equalsIgnoreCase("POST")
        && "register".equals(request.getParameter("action"))) {
%>
        <p style="font-size:medium">
        Thank you very much for your interest. We will contact you shortly
        via email!
        </p>
<%  } else if (e instanceof DisabledException) { %>
        Your portal account has been disabled.<br>
        If you believe this is an error please contact the site administrators.
<%  } else if (e instanceof UsernameNotFoundException) { %>
        The Virtual Rock Laboratory is only accessible by registered users.<br>
<%      if (request.getHeader("Shib-Person-commonName") == null) { %>
        Unfortunately, your institution (IdP) does not release the details
        required to securely authenticate yourself.<br/>
        Please send your details to: <a href="mailto:webmaster@esscc.uq.edu.au">webmaster@esscc.uq.edu.au</a>
        and we will be in touch with you shortly.
<%      } else { %>
        If you would like to register now please use the button below.<br>
        The following details will be forwarded to the administrators who will
        be in touch with you via email shortly:<br>
        <table class="att-table">
            <tr><th>Name:</th><td><%=request.getHeader("Shib-Person-commonName")%></td></tr>
            <tr><th>Email:</th><td><%=request.getRemoteUser()%></td></tr>
            <tr><th>Organisation:</th><td><%=request.getHeader("Shib-EP-OrgDN")%></td></tr>
            <tr><th>Affiliation:</th><td><%=request.getHeader("Shib-EP-Affiliation")%></td></tr>
            <tr><th>Shared Token:</th><td><%=request.getHeader("Shib-AuEduPerson-SharedToken")
%></td></tr>
        </table>
        <form action="access_error.html" method="POST">
            <input type="hidden" name="action" value="register" />
            <input type="submit" value="Register" />
        </form>
<%      }
    } else { %>
        You are not allowed to access this page.<br>
        If you believe this is an error please contact the site administrators.
<%  } %>
        </p>
    </div>
</body>

</html>

