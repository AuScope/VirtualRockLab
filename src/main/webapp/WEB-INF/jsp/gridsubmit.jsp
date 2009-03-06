<%@ include file="/WEB-INF/jsp/include.jsp" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<html>
<head>
  <title><fmt:message key="title"/></title>
  <style>
    .error { color: red; }
  </style>

  <script language="javascript">
      var allVersions;
      var inTransfers;
      var numUploads=0;

      init = function() {
          inTransfers = new Array();
          <c:forEach items="${gridSubmit.inTransfers}" var="xfer">
          inTransfers.push("${xfer}");</c:forEach>
          updateTransfers();

          var versionsOnSite;
          allVersions = new Array();
          <c:forEach items="${versions}" var="site">
          versionsOnSite = new Array();
          <c:forEach items="${site}" var="version">
          versionsOnSite.push("${version}");</c:forEach>
          allVersions.push(versionsOnSite);
          </c:forEach>
          updateVersions();
      }

      removeTransfer = function(index) {
          inTransfers.splice(index,1);
          updateTransfers();
      }

      appendTransfer = function() {
          inTransfers.push("gsiftp://");
          updateTransfers();
      }

      removeFileUpload = function(index) {
          el = document.getElementById("divupload"+index);
          el.parentNode.removeChild(el);
      }

      appendFileUpload = function() {
          fuDiv = document.getElementById("fileUploadDiv");
          subDiv = document.createElement("div");
          subDiv.id = "divupload"+numUploads;
          fuDiv.appendChild(subDiv);
          trEl = document.createElement("tr");
          subDiv.appendChild(trEl);
          tdEl = document.createElement("td");
          trEl.appendChild(tdEl);
          xferElem = document.createElement("input");
          xferElem.id = "file"+numUploads;
          xferElem.type = "File";
          tdEl.appendChild(xferElem);
          tdEl = document.createElement("td");
          trEl.appendChild(tdEl);
          delBtn = document.createElement("input");
          delBtn.type = "button";
          delBtn.value = "Remove";
          var idx = numUploads;
          delBtn.onclick = function() { removeFileUpload(idx); };
          tdEl.appendChild(delBtn);
          numUploads++;
      }

      addTransferElement = function(parent, index) {
          trEl = document.createElement("tr");
          parent.appendChild(trEl);
          tdEl = document.createElement("td");
          trEl.appendChild(tdEl);
          xferElem = document.createElement("input");
          xferElem.id = "xfer"+index;
          xferElem.value = inTransfers[index];
          tdEl.appendChild(xferElem);
          tdEl = document.createElement("td");
          trEl.appendChild(tdEl);
          delBtn = document.createElement("input");
          delBtn.type = "button";
          delBtn.value = "Remove";
          delBtn.onclick = function() { removeTransfer(index); };
          tdEl.appendChild(delBtn);
      }

      updateTransfers = function() {
          xferDiv = document.getElementById("transfersDiv");
          subDiv = document.createElement("div");
          xferDiv.replaceChild(subDiv, xferDiv.childNodes[0]);

          // skip first element (local stageIn)
          for (var i=1; i<inTransfers.length; i++) {
              addTransferElement(subDiv, i);
          }
      }

      updateVersions = function() {
          var idx = document.jobForm.site.selectedIndex;
          document.jobForm.version.length = 0;
          for (var v=0; v < allVersions[idx].length; v++) {
              document.jobForm.version[v] = new Option(allVersions[idx][v]);
          }
      }

      beforeSubmit = function() {
          // skip first element (keep local stageIn)
          for (var i=1; i<inTransfers.length; i++) {
              inTransfers[i] = document.getElementById("xfer"+i).value;
          }
          inputs = document.getElementsByTagName("input");
          numUploads = 0;
          for (var i=0; i<inputs.length; i++) {
              if (inputs[i].type == "file") {
                  inputs[i].name = "file"+numUploads;
                  numUploads++;
              }
          }
          var formXfers=document.getElementById("inTransfers");
          formXfers.value = inTransfers;
          document.jobForm.submit();
      }
  </script>
</head>

<body onload="init()">
    <form:form method="post" commandName="gridSubmit" name="jobForm" enctype="multipart/form-data">
        <form:hidden id="inTransfers" path="inTransfers"/>
        <h1>Submit a simulation job</h1>
        <table width="100%" bgcolor="f8f8ff" border="0" cellspacing="0" cellpadding="5">
        <tr>
            <td align="right" width="25%">Job Name:</td>
            <td width="25%"><form:input path="name"/></td>
            <td width="50%"><form:errors path="name" cssClass="error"/></td>
        </tr>
        <tr>
            <td align="right" width="25%">Site:</td>
            <td width="25%">
                <form:select path="site" onchange="updateVersions()">
                    <c:forEach items="${sites}" var="site">
                        <form:option label="${site}" value="${site}"/>
                    </c:forEach>
                </form:select>
            </td>
            <td width="50%"><form:errors path="site" cssClass="error"/></td>
        </tr>
        <tr>
            <td align="right" width="25%">ESyS-Particle Version:</td>
            <td width="25%">
                <form:select path="version">
                </form:select>
            </td>
            <td width="50%"><form:errors path="version" cssClass="error"/></td>
        </tr>
        <tr>
            <td align="right">Files to Stage In:</td>
        </tr>
        <tr><td/>
            <td>
                <input type="button" value="Add FTP Link" onClick="appendTransfer()"/>
            </td>
        </tr>
        <tr><td/><td><div id="transfersDiv"/><div/></td></tr>
        <tr><td/>
            <td>
                <input type="button" value="Add File Upload" onClick="appendFileUpload()"/>
            </td>
        </tr>
        <tr><td/><td colspan="2"><div id="fileUploadDiv"/><div/></td></tr>
        <tr>
            <td align="right" width="25%">Script Filename:</td>
            <td width="25%">
                <form:input path="arguments[0]"/>
            </td>
            <td width="50%"><form:errors path="arguments" cssClass="error"/></td>
        </tr>
        </table>
        <br>
        <input type="button" align="center" value="Submit" onClick="beforeSubmit()">
        <h3><form:errors cssClass="error"/></h3>
    </form:form>
    <a href="<c:url value="index.html"/>">Home</a>
</body>

</html>

