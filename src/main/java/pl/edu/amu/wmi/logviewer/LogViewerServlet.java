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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final int PAGE_SIZE = 2000;
    private static final Predicate<String> LOG_FILE_FILTER = name -> name.endsWith(".log") || name.endsWith(".txt") || name.equals("catalina.out");

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
        long totalLines = countLinesWithFallback(file);

        // Default to last page if no page specified
        int pageNum = getPageNumber(req);
        if (pageNum <= 0) {
            pageNum = (int) Math.ceil((double) totalLines / PAGE_SIZE);
            pageNum = Math.max(1, pageNum);  // Ensure at least page 1
        }

        List<String> lines = readFileLinesWithFallback(file, pageNum);

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

    private List<String> readFileLinesWithFallback(File file, int pageNum) throws IOException {
        try {
            return readPage(file, pageNum, StandardCharsets.UTF_8);
        } catch (UncheckedIOException | MalformedInputException e) {
            if (isDecodingIssue(e)) {
                return readPage(file, pageNum, StandardCharsets.ISO_8859_1);
            }
            throw e;
        }
    }

    private List<String> readPage(File file, int pageNum, Charset charset) throws IOException {
        int toSkip = Math.max(0, (pageNum - 1) * PAGE_SIZE);
        List<String> result = new ArrayList<>(PAGE_SIZE);
        try (BufferedReader reader = newTolerantReader(file.toPath(), charset)) {
            for (int i = 0; i < toSkip; i++) {
                if (reader.readLine() == null) {
                    break;
                }
            }
            for (int i = 0; i < PAGE_SIZE; i++) {
                String line = reader.readLine();
                if (line == null) break;
                result.add(line);
            }
        }
        return result;
    }

    private long countLinesWithFallback(File file) throws IOException {
        try (BufferedReader reader = newTolerantReader(file.toPath(), StandardCharsets.UTF_8)) {
            return reader.lines().count();
        }
    }

    private BufferedReader newTolerantReader(Path path, java.nio.charset.Charset charset) throws IOException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return new BufferedReader(new InputStreamReader(Files.newInputStream(path), decoder));
    }

    private boolean isDecodingIssue(Throwable e) {
        if (e instanceof MalformedInputException) return true;
        Throwable cause = e.getCause();
        return cause instanceof MalformedInputException;
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
