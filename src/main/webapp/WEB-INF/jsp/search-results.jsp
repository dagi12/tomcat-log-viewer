%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Search Results - "${searchTerm}"</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <h1>Search Results</h1>

    <div class="search-info">
        <p>
            Search term: <strong>"${searchTerm}"</strong>
            <c:if test="${not empty fileName}">
                in file: <strong>${fileName}</strong>
            </c:if>
        </p>
    </div>

    <div class="controls">
        <a href="${pageContext.request.contextPath}/logs" class="button">Back to Log List</a>
        <c:if test="${not empty fileName}">
            <a href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}" class="button">View Full Log</a>
        </c:if>
    </div>

    <div class="search-results">
        <p>${results.size()} result(s) found</p>

        <c:if test="${not empty results}">
            <table class="result-table">
                <thead>
                <tr>
                    <th>File</th>
                    <th>Line</th>
                    <th>Content</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${results}" var="result">
                    <tr>
                        <td>${result.fileName}</td>
                        <td>${result.lineNumber}</td>
                        <td class="line-content">${result.line}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
        <c:if test="${empty results}">
            <p class="no-results">No results found for "${searchTerm}"</p>
        </c:if>
    </div>

    <div class="footer">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </div>
</div>
</body>
</html>
