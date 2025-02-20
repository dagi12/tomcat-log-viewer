<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
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
        <%-- Display specific error message if available --%>
        <% if (request.getAttribute("error") != null) { %>
        <p>${error}</p>
        <% } else if (exception != null) { %>
        <p>An error occurred: <%= exception.getMessage() %></p>
        <pre class="error-stack">
                    <% for (StackTraceElement element : exception.getStackTrace()) { %>
                        <%= element.toString() %>
                    <% } %>
                </pre>
        <% } else { %>
        <p>An unexpected error occurred. Please check the server logs for more details.</p>
        <% } %>
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
