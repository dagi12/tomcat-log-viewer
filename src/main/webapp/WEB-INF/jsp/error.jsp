<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Error - Tomcat Log Viewer</title>
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <div class="text-center mb-4">
                <i class="bi bi-exclamation-triangle text-danger" style="font-size: 4rem;"></i>
                <h1 class="mt-3">Error</h1>
            </div>

            <div class="card">
                <div class="card-body">
                    <div class="alert alert-danger mb-4">
                        <%-- Display specific error message if available --%>
                        <% if (request.getAttribute("error") != null) { %>
                        <p class="mb-0">${error}</p>
                        <% } else if (exception != null) { %>
                        <p class="mb-0">An error occurred: <%= exception.getMessage() %>
                        </p>
                        <div class="mt-3">
                                    <pre class="bg-light p-3 rounded" style="font-size: 0.875rem;">
                                        <% for (StackTraceElement element : exception.getStackTrace()) { %>
<%= element.toString() %>
                                        <% } %>
                                    </pre>
                        </div>
                        <% } else { %>
                        <p class="mb-0">An unexpected error occurred. Please check the server logs for more details.</p>
                        <% } %>
                    </div>

                    <div class="text-center">
                        <a href="${pageContext.request.contextPath}/logs" class="btn btn-primary">
                            <i class="bi bi-arrow-left"></i> Back to Log List
                        </a>
                    </div>
                </div>
            </div>

            <footer class="text-center text-muted mt-4">
                <p>&copy; 2025 Tomcat Log Viewer</p>
            </footer>
        </div>
    </div>
</div>

<!-- Bootstrap 5 JS Bundle -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
