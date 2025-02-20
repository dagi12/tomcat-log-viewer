<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Error - Tomcat Log Viewer</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
</head>
<body>
<div class="container error-container">
    <h1>Error</h1>
    <div class="error-message">
        <p>${error}</p>
    </div>
    <div class="controls">
        <a href="${pageContext.request.contextPath}/logs" class="button">Back to Log List</a>
    </div>
    <div class="footer">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </div>
</div>
</body>
</html>
