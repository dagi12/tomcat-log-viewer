<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tomcat Log Viewer</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
    <h1 class="mb-4">Tomcat Log Viewer</h1>

    <div class="alert alert-info">
        <i class="bi bi-folder"></i> Log Directory: <code>${logDir}</code>
    </div>

    <div class="card">
        <div class="table-responsive">
            <table class="table table-hover table-striped mb-0">
                <thead class="table-light">
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
                        <td>
                            <i class="bi bi-file-text"></i>
                                ${logFile.name}
                        </td>
                        <td>${logFile.size}</td>
                        <td>${logFile.lastModified}</td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                <a href="${pageContext.request.contextPath}/logs?action=view&file=${logFile.name}"
                                   class="btn btn-outline-primary">
                                    <i class="bi bi-eye"></i> View
                                </a>
                                <a href="${pageContext.request.contextPath}/logs?action=download&file=${logFile.name}"
                                   class="btn btn-outline-success">
                                    <i class="bi bi-download"></i> Download
                                </a>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty logFiles}">
                    <tr>
                        <td colspan="4" class="text-center text-muted py-4">
                            <i class="bi bi-inbox h1 d-block"></i>
                            No log files found
                        </td>
                    </tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <footer class="text-center text-muted mt-4">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </footer>
</div>

<!-- Bootstrap 5 JS Bundle -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
