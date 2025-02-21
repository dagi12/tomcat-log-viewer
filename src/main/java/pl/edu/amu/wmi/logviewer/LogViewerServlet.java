package pl.edu.amu.wmi.logviewer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@WebServlet("/logs")
public class LogViewerServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(LogViewerServlet.class);
    private static final String DEFAULT_LOG_DIR = System.getProperty("catalina.base") + "/logs";
    private static final int PAGE_SIZE = 1000;
    private static final Predicate<String> LOG_FILE_FILTER = name -> name.endsWith(".log") || name.endsWith(".txt");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String action = req.getParameter("action");
            if ("view".equals(action)) viewLog(req, resp);
            else if ("download".equals(action)) downloadLog(req, resp);
            else listLogs(req, resp);
        } catch (Exception e) {
            log.error("Error processing request", e);
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(req, resp);
        }
    }

    private void listLogs(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String logDir = DEFAULT_LOG_DIR;
        File dir = new File(logDir);

        if (!dir.exists() || !dir.isDirectory()) {
            throw new ServletException("Log directory not found: " + logDir);
        }

        List<LogFile> logFiles = Arrays.stream(Objects.requireNonNull(dir.listFiles((d, name) -> LOG_FILE_FILTER.test(name))))
                .map(this::createLogFile)
                .sorted((a, b) -> b.getLastModified().compareTo(a.getLastModified()))
                .collect(Collectors.toList());

        req.setAttribute("logDir", logDir);
        req.setAttribute("logFiles", logFiles);
        req.getRequestDispatcher("/WEB-INF/jsp/list-logs.jsp").forward(req, resp);
    }

    private void viewLog(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = validateFileName(req.getParameter("file"));
        File file = new File(DEFAULT_LOG_DIR, fileName);
        int pageNum = Math.max(1, getPageNumber(req));

        long totalLines = Files.lines(file.toPath()).count();
        List<String> lines = readFileLines(file, pageNum);

        req.setAttribute("fileName", fileName);
        req.setAttribute("logContent", lines);
        req.setAttribute("currentPage", pageNum);
        req.setAttribute("totalPages", (int) Math.ceil((double) totalLines / PAGE_SIZE));
        req.setAttribute("fileSize", formatFileSize(file.length()));
        req.getRequestDispatcher("/WEB-INF/jsp/view-log.jsp").forward(req, resp);
    }

    private void downloadLog(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = validateFileName(req.getParameter("file"));
        File file = new File(DEFAULT_LOG_DIR, fileName);

        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setContentLength((int) file.length());

        try (InputStream in = FileUtils.openInputStream(file)) {
            IOUtils.copy(in, resp.getOutputStream());
        }
    }

    private String validateFileName(String fileName) throws ServletException, IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new ServletException("File name not provided");
        }

        File requestedFile = new File(LogViewerServlet.DEFAULT_LOG_DIR, fileName);
        if (!requestedFile.getCanonicalPath().startsWith(new File(LogViewerServlet.DEFAULT_LOG_DIR).getCanonicalPath())) {
            throw new ServletException("Access denied: Invalid file path");
        }

        if (!requestedFile.exists() || !requestedFile.isFile()) {
            throw new ServletException("Log file not found: " + fileName);
        }

        return fileName;
    }

    private List<String> readFileLines(File file, int pageNum) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.lines().limit((long) (pageNum - 1) * PAGE_SIZE).forEach(l -> {
            });
            return reader.lines().limit(PAGE_SIZE).collect(Collectors.toList());
        }
    }

    private int getPageNumber(HttpServletRequest req) {
        try {
            return Integer.parseInt(req.getParameter("page"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private LogFile createLogFile(File file) {
        LogFile logFile = new LogFile();
        logFile.setName(file.getName());
        logFile.setSize(formatFileSize(file.length()));
        logFile.setLastModified(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())));
        logFile.setPath(file.getAbsolutePath());
        return logFile;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    @Data
    public static class LogFile {
        private String name;
        private String size;
        private String lastModified;
        private String path;
    }
}
