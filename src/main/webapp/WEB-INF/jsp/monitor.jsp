<%@ include file="/WEB-INF/jsp/include.jsp" %>

<html>
    <head>
        <title><fmt:message key="title"/></title>
        <style>
            .error { color: red; font-weight: bold }
            .active { color: green; font-weight: bold }
        </style>  

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
        <h1><fmt:message key="title"/></h1>
        <p><a href="<c:url value="monitor.html"/>"><fmt:message key="home"/></a></p>
        <p><a href="<c:url value="scriptbuilder.html"/>"><fmt:message key="createscript"/></a></p>
        <p><a href="<c:url value="gridsubmit.html"/>"><fmt:message key="submitjob"/></a></p>
        <p><a href="<c:url value="query.html"/>"><fmt:message key="queryjobs"/></a></p>
        <br>
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

