package com.logviewer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@WebServlet("/logs")
public class LogViewerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_LOG_DIR = System.getProperty("catalina.base") + "/logs";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if ("view".equals(action)) {
            viewLog(request, response);
        } else if ("download".equals(action)) {
            downloadLog(request, response);
        } else if ("search".equals(action)) {
            searchLogs(request, response);
        } else {
            listLogs(request, response);
        }
    }

    private void listLogs(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Get custom log directory if provided in application config
        String logDir = getServletContext().getInitParameter("log.directory");
        if (logDir == null || logDir.isEmpty()) {
            logDir = DEFAULT_LOG_DIR;
        }

        try {
            List<LogFile> logFiles = new ArrayList<>();
            File dir = new File(logDir);

            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".log") || name.endsWith(".txt"));

                if (files != null) {
                    for (File file : files) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        LogFile logFile = new LogFile();
                        logFile.setName(file.getName());
                        logFile.setSize(formatFileSize(file.length()));
                        logFile.setLastModified(sdf.format(new Date(file.lastModified())));
                        logFile.setPath(file.getAbsolutePath());
                        logFiles.add(logFile);
                    }
                }

                // Sort by last modified date (newest first)
                Collections.sort(logFiles, (a, b) -> b.getLastModified().compareTo(a.getLastModified()));
            }

            request.setAttribute("logDir", logDir);
            request.setAttribute("logFiles", logFiles);
            request.getRequestDispatcher("/WEB-INF/jsp/list-logs.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", "Error listing log files: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void viewLog(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fileName = request.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logs");
            return;
        }

        String logDir = getServletContext().getInitParameter("log.directory");
        if (logDir == null || logDir.isEmpty()) {
            logDir = DEFAULT_LOG_DIR;
        }

        // Prevent directory traversal attacks
        File requestedFile = new File(logDir, fileName);
        if (!requestedFile.getCanonicalPath().startsWith(new File(logDir).getCanonicalPath())) {
            request.setAttribute("error", "Access denied: Invalid file path");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
            return;
        }

        // Parameters for pagination
        int pageSize = 1000; // lines per page
        int pageNum = 1;
        try {
            String page = request.getParameter("page");
            if (page != null && !page.isEmpty()) {
                pageNum = Integer.parseInt(page);
                if (pageNum < 1) pageNum = 1;
            }
        } catch (NumberFormatException e) {
            pageNum = 1;
        }

        try {
            File file = new File(logDir, fileName);
            if (!file.exists() || !file.isFile()) {
                request.setAttribute("error", "Log file not found: " + fileName);
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }

            // Count total lines for pagination
            long totalLines = Files.lines(file.toPath()).count();
            int totalPages = (int) Math.ceil((double) totalLines / pageSize);

            // Read the requested page
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                // Skip lines before the requested page
                for (int i = 0; i < (pageNum - 1) * pageSize; i++) {
                    if (reader.readLine() == null) break;
                }

                // Read lines for the current page
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < pageSize) {
                    lines.add(line);
                    count++;
                }
            }

            request.setAttribute("fileName", fileName);
            request.setAttribute("logContent", lines);
            request.setAttribute("currentPage", pageNum);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("fileSize", formatFileSize(file.length()));
            request.getRequestDispatcher("/WEB-INF/jsp/view-log.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", "Error reading log file: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void downloadLog(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String fileName = request.getParameter("file");
        if (fileName == null || fileName.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/logs");
            return;
        }

        String logDir = getServletContext().getInitParameter("log.directory");
        if (logDir == null || logDir.isEmpty()) {
            logDir = DEFAULT_LOG_DIR;
        }

        // Prevent directory traversal attacks
        File requestedFile = new File(logDir, fileName);
        if (!requestedFile.getCanonicalPath().startsWith(new File(logDir).getCanonicalPath())) {
            request.setAttribute("error", "Access denied: Invalid file path");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
            return;
        }

        try {
            File file = new File(logDir, fileName);
            if (!file.exists() || !file.isFile()) {
                request.setAttribute("error", "Log file not found: " + fileName);
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            response.setContentLength((int) file.length());

            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            request.setAttribute("error", "Error downloading log file: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void searchLogs(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String searchTerm = request.getParameter("term");
        String fileName = request.getParameter("file");

        if (searchTerm == null || searchTerm.isEmpty()) {
            if (fileName != null && !fileName.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/logs?action=view&file=" + fileName);
            } else {
                response.sendRedirect(request.getContextPath() + "/logs");
            }
            return;
        }

        String logDir = getServletContext().getInitParameter("log.directory");
        if (logDir == null || logDir.isEmpty()) {
            logDir = DEFAULT_LOG_DIR;
        }

        try {
            List<SearchResult> results = new ArrayList<>();

            if (fileName != null && !fileName.isEmpty()) {
                // Search in specific file
                // Prevent directory traversal attacks
                File requestedFile = new File(logDir, fileName);
                if (!requestedFile.getCanonicalPath().startsWith(new File(logDir).getCanonicalPath())) {
                    request.setAttribute("error", "Access denied: Invalid file path");
                    request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                    return;
                }

                File file = new File(logDir, fileName);
                if (file.exists() && file.isFile()) {
                    searchInFile(file, searchTerm, results);
                }
            } else {
                // Search in all log files
                File dir = new File(logDir);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".log") || name.endsWith(".txt"));
                    if (files != null) {
                        for (File file : files) {
                            searchInFile(file, searchTerm, results);
                            // Limit results to prevent overwhelming the UI
                            if (results.size() >= 1000) break;
                        }
                    }
                }
            }

            request.setAttribute("searchTerm", searchTerm);
            request.setAttribute("fileName", fileName);
            request.setAttribute("results", results);
            request.getRequestDispatcher("/WEB-INF/jsp/search-results.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", "Error searching log files: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void searchInFile(File file, String searchTerm, List<SearchResult> results) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.toLowerCase().contains(searchTerm.toLowerCase())) {
                    SearchResult result = new SearchResult();
                    result.setFileName(file.getName());
                    result.setLineNumber(lineNumber);
                    result.setLine(line);
                    results.add(result);

                    // Limit results per file to prevent memory issues
                    if (results.size() >= 1000) break;
                }
            }
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    // Model classes
    public static class LogFile {
        private String name;
        private String size;
        private String lastModified;
        private String path;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class SearchResult {
        private String fileName;
        private int lineNumber;
        private String line;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getLine() {
            return line;
        }

        public void setLine(String line) {
            this.line = line;
        }
    }
}
