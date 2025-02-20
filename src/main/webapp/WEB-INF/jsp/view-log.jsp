<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tomcat Log Viewer</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container">
    <h1>Tomcat Log Viewer</h1>

    <div class="search-bar">
        <form action="${pageContext.request.contextPath}/logs" method="get">
            <input type="hidden" name="action" value="search">
            <input type="text" name="term" placeholder="Search all logs..." required>
            <button type="submit">Search</button>
        </form>
    </div>

    <div class="log-directory">
        <p>Log Directory: <code>${logDir}</code></p>
    </div>

    <table class="log-table">
        <thead>
        <tr>
            <th>File Name</th>
            <th>Size</th>
            <th>Last Modified</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${logFiles}" var="logFile">
            <tr>
                <td>${logFile.name}</td>
                <td>${logFile.size}</td>
                <td>${logFile.lastModified}</td>
                <td class="actions">
                    <a href="${pageContext.request.contextPath}/logs?action=view&file=${logFile.name}" class="button">View</a>
                    <a href="${pageContext.request.contextPath}/logs?action=download&file=${logFile.name}" class="button">Download</a>
                    <a href="${pageContext.request.contextPath}/logs?action=search&file=${logFile.name}" class="button">Search</a>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty logFiles}">
            <tr>
                <td colspan="4" class="empty-message">No log files found</td>
            </tr>
        </c:if>
        </tbody>
    </table>

    <div class="footer">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </div>
</div>
</body>
</html>
