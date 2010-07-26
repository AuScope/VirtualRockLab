<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>

<head>
    <title>Virtual Rock Laboratory - Access Denied</title>
    <link rel="stylesheet" type="text/css" href="css/vrl.min.css">
    <style type="text/css">
      .error { font-size:medium; font-weight:bold; color:red; }
      .confirm { font-size:medium; font-weight:bold; color:blue; }
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
            <img alt="" src="<%=request.getContextPath()%>/img/img-auscope-banner.gif" />
        </div>
    </div>

    <div id="body" style="padding:10px;">
<c:choose>
    <c:when test="${show=='accountDisabled'}">
        <p class="error">Access Denied</p>
        <p style="font-size:small">
        Your portal account has been disabled.<br>
        If you believe this is an error please contact the site administrators.
        </p>
    </c:when>
    <c:when test="${show=='notRegistered'}">
        <p class="error">Access Denied</p>
        <p style="font-size:small">
        The Virtual Rock Laboratory is only accessible by registered users.<br>
        <c:choose>
            <c:when test="${commonName==null}">
        Unfortunately, your institution (IdP) does not release the details
        required to securely authenticate yourself.<br/>
        Please send your details to: <a href="mailto:<c:out value="${notify}"/>"><c:out value="${notify}"/></a>
        and we will be in touch with you shortly.
        </p>
            </c:when>
            <c:otherwise>
        If you would like to register now please use the button below.<br>
        The following details will be forwarded to the administrators who will
        be in touch with you via email shortly:<br>
        <table class="att-table">
            <tr><th>Name:</th><td><c:out value="${commonName}"/></td></tr>
            <tr><th>Email:</th><td><c:out value="${email}"/></td></tr>
            <tr><th>Organisation:</th><td><c:out value="${organisation}"/></td></tr>
            <tr><th>Affiliation:</th><td><c:out value="${affiliation}"/></td></tr>
            <tr><th>Shared Token:</th><td><c:out value="${sharedToken}"/></td></tr>
        </table>
        <form action="access_error.html" method="POST">
            <input type="hidden" name="action" value="register" />
            <input type="submit" value="Register" />
        </form>
        </p>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:when test="${show=='messageSent'}">
        <p class="confirm">Notification Sent</p>
        <p style="font-size:medium">
        Thank you very much for your interest. We will contact you shortly
        via email!
        </p>
    </c:when>
    <c:when test="${show=='messageSendError'}">
        <p class="error">Internal Error</p>
        <p style="font-size:medium">
        Unable to send message. Please contact our administrators directly by
        emailing <c:out value="${notify}"/>.
        </p>
    </c:when>
    <c:otherwise>
        <p class="error">Access Denied</p>
        <p style="font-size:small">
        You are not allowed to access this page.<br>
        If you believe this is an error please contact the site administrators.
        </p>
    </c:otherwise>
</c:choose>
    </div>
</body>

</html>

