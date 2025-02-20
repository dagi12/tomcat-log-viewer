<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>View Log - ${fileName}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <h1>Log File: ${fileName}</h1>

    <div class="controls">
        <a href="${pageContext.request.contextPath}/logs" class="button">Back to Log List</a>
        <a href="${pageContext.request.contextPath}/logs?action=download&file=${fileName}" class="button">Download</a>

        <div class="search-bar">
            <form action="${pageContext.request.contextPath}/logs" method="get">
                <input type="hidden" name="action" value="search">
                <input type="hidden" name="file" value="${fileName}">
                <input type="text" name="term" placeholder="Search in this file..." required>
                <button type="submit">Search</button>
            </form>
        </div>
    </div>

    <div class="file-info">
        <p>File Size: ${fileSize}</p>
    </div>

    <div class="pagination">
        <c:if test="${currentPage > 1}">
            <a href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage - 1}" class="button">Previous</a>
        </c:if>
        <span>Page ${currentPage} of ${totalPages}</span>
        <c:if test="${currentPage < totalPages}">
            <a href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage + 1}" class="button">Next</a>
        </c:if>
    </div>

    <div class="log-content">
            <pre><code><c:forEach items="${logContent}" var="line">${line}
            </c:forEach></code></pre>
    </div>

    <div class="pagination">
        <c:if test="${currentPage > 1}">
            <a href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage - 1}" class="button">Previous</a>
        </c:if>
        <span>Page ${currentPage} of ${totalPages}</span>
        <c:if test="${currentPage < totalPages}">
            <a href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage + 1}" class="button">Next</a>
        </c:if>
    </div>

    <div class="footer">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </div>
</div>
</body>
</html>
