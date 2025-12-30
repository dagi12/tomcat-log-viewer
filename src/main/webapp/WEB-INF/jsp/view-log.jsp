<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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
    <style>
        body {
            font-size: 0.9rem;
        }

        .log-content {
            font-family: 'Courier New', monospace;
            font-size: 0.85rem;
            line-height: 1.3;
            margin: 0;
            padding: 1rem;
            background-color: #f8f9fa;
            border-radius: 0.25rem;
            white-space: pre-wrap;
            word-wrap: break-word;
            white-space: -moz-pre-wrap;
            white-space: -pre-wrap;
            white-space: -o-pre-wrap;
        }

        .card {
            border: 1px solid rgba(0, 0, 0, .125);
            box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, .075);
        }

        .alert {
            font-size: 0.85rem;
            padding: 0.5rem 1rem;
        }

        .pagination {
            margin: 0.5rem 0;
        }

        .page-link {
            padding: 0.25rem 0.5rem;
            font-size: 0.85rem;
        }

        h1 {
            font-size: 1.5rem;
            font-weight: 600;
        }
    </style>
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
                <pre class="log-content"
                     style="max-height: 70vh; overflow-y: auto; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; white-space: -o-pre-wrap; word-wrap: break-word;"><c:forEach
                        items="${logContent}" var="line">${fn:replace(fn:replace(line, '<', '&lt;'), '>', '&gt;')}
                </c:forEach></pre>
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
