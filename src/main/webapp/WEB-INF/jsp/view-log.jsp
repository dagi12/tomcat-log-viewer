<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>View Log - ${fileName}</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="mb-0">
            <i class="bi bi-file-text"></i>
            ${fileName}
        </h1>
        <div class="btn-group">
            <a href="${pageContext.request.contextPath}/logs" class="btn btn-outline-primary">
                <i class="bi bi-arrow-left"></i> Back to Log List
            </a>
            <a href="${pageContext.request.contextPath}/logs?action=download&file=${fileName}"
               class="btn btn-outline-success">
                <i class="bi bi-download"></i> Download
            </a>
        </div>
    </div>

    <div class="alert alert-info">
        <i class="bi bi-info-circle"></i> File Size: ${fileSize}
    </div>

    <nav aria-label="Log navigation" class="mb-3">
        <ul class="pagination justify-content-center">
            <c:if test="${currentPage > 1}">
                <li class="page-item">
                    <a class="page-link"
                       href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage - 1}">
                        <i class="bi bi-chevron-left"></i> Previous
                    </a>
                </li>
            </c:if>
            <li class="page-item disabled">
                <span class="page-link">Page ${currentPage} of ${totalPages}</span>
            </li>
            <c:if test="${currentPage < totalPages}">
                <li class="page-item">
                    <a class="page-link"
                       href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage + 1}">
                        Next <i class="bi bi-chevron-right"></i>
                    </a>
                </li>
            </c:if>
        </ul>
    </nav>

    <div class="card">
        <div class="card-body p-0">
                <pre class="mb-0" style="max-height: 70vh; overflow-y: auto;"><code><c:forEach items="${logContent}"
                                                                                               var="line">${line}
                </c:forEach></code></pre>
        </div>
    </div>

    <nav aria-label="Log navigation" class="mt-3">
        <ul class="pagination justify-content-center">
            <c:if test="${currentPage > 1}">
                <li class="page-item">
                    <a class="page-link"
                       href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage - 1}">
                        <i class="bi bi-chevron-left"></i> Previous
                    </a>
                </li>
            </c:if>
            <li class="page-item disabled">
                <span class="page-link">Page ${currentPage} of ${totalPages}</span>
            </li>
            <c:if test="${currentPage < totalPages}">
                <li class="page-item">
                    <a class="page-link"
                       href="${pageContext.request.contextPath}/logs?action=view&file=${fileName}&page=${currentPage + 1}">
                        Next <i class="bi bi-chevron-right"></i>
                    </a>
                </li>
            </c:if>
        </ul>
    </nav>

    <footer class="text-center text-muted mt-4">
        <p>&copy; 2025 Tomcat Log Viewer</p>
    </footer>
</div>

<!-- Bootstrap 5 JS Bundle -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
