<%@ include file="/WEB-INF/jsp/include.jsp" %>

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

    <script language="javascript">
        downloadFile = function(ref, file) {
            document.forms[0].action.value = "downloadFile";
            document.forms[0].ref.value = ref;
            document.forms[0].filename.value = file;
            document.forms[0].submit();
        }
    </script>
</head>

<body>
    <%@ include file="page_header.jsp" %>
    <div id="body"></div>

    <c:if test="${model.files != null}">
    <h3>Job Files</h3>
    <form method="POST" action="<c:url value="monitor.html"/>">
        <input type="hidden" name="action" value=""/>
        <input type="hidden" name="ref" value=""/>
        <input type="hidden" name="filename" value=""/>
    </form>
    <table border>
        <tr>
            <th>Filename</th>
            <th>File size (Bytes)</th>
            <th>Actions</th>
        </tr>
        <c:forEach items="${model.files}" var="file">
        <tr>
            <td><c:out value="${file.name}"/></td>
            <td align="right"><c:out value="${file.readableSize}"/></td>
            <td>
                <input type="button" value="Download" onClick="downloadFile('<c:out value="${model.ref}"/>','<c:out value="${file.name}"/>')"/>
            </td>
        </tr>
        </c:forEach>
    </table>
    </c:if>
</body>

</html>

