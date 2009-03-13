<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ include file="include.jsp" %>

<html>

<head>
    <title><fmt:message key="title"/></title>
    <link rel="stylesheet" type="text/css" href="css/virtualrocklab.css">
    <style type="text/css">
      #sitenav-01 a {
        background: url( "/img/navigation.gif" ) 0px -38px no-repeat;
      }
      .error { color: red; font-weight: bold }
      .active { color: green; font-weight: bold }
    </style>
    <link rel="stylesheet" type="text/css" href="js/ext/resources/css/ext-all.css">
    <script type="text/javascript" src="js/ext/adapter/ext/ext-base.js"></script>
    <script type="text/javascript" src="js/ext/ext-all.js"></script>

    <script language="javascript">
        jobDetails = function(ref) {
            document.forms[0].action.value = "jobDetails";
            document.forms[0].ref.value = ref;
            document.forms[0].submit();
        }

        killJob = function(ref) {
            if (confirm("Are you sure you want to kill the job?")) {
                document.forms[0].action.value = "killJob";
                document.forms[0].ref.value = ref;
                document.forms[0].submit();
            }
        }
    </script>
</head>

<body>
    <%@ include file="page_header.jsp" %>
    <div id="body"></div>

    <c:if test="${model.jobs != null}">
    <h3>Your Grid Jobs</h3>
    <form method="POST" action="<c:url value="monitor.html"/>">
        <input type="hidden" name="action" value=""/>
        <input type="hidden" name="ref" value=""/>
    </form>
    <table border>
        <tr>
            <th>Job Name</th>
            <th>Submit Date</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        <c:forEach items="${model.jobs}" var="job">
        <tr>
            <td><c:out value="${job.name}"/></td>
            <td><c:out value="${job.timeStamp}"/></td>
            <td
            <c:if test="${job.status == 'Failed'}"> class='error'</c:if>
            <c:if test="${job.status == 'Active'}"> class='active'</c:if>
            ><c:out value="${job.status}"/></td>
            <td>
                <input type="button" value="Details" onClick="jobDetails('<c:out value="${job.reference}"/>')"/>
            <c:if test="${job.status == 'Active'}">
                <input type="button" value="Kill" onClick="killJob('<c:out value="${job.reference}"/>')"/>
            </c:if>
            &nbsp;</td>
        </tr>
        </c:forEach>
    </table>
    </c:if>
</body>

</html>

