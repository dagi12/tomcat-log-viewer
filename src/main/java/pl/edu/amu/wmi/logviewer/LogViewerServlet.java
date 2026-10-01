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
import java.io.PrintWriter;
import java.io.RandomAccessFile;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@WebServlet("/logs")
public class LogViewerServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(LogViewerServlet.class);
    private static final String DEFAULT_TAIL_FILE = "catalina.out";
    private static final int PAGE_SIZE = 2000;
    private static final int DEFAULT_TAIL_LINES = 200;
    private static final int MAX_TAIL_LINES = 2000;

    private String logDir() {
        return System.getProperty("catalina.base") + "/logs";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("tail".equals(action) || "list".equals(action)) {
            try {
                if ("list".equals(action)) {
                    listLogsJson(resp);
                } else {
                    tailLogJson(req, resp);
                }
            } catch (Exception e) {
                log.error("Error processing JSON request", e);
                writeJsonError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            return;
        }
        try {
            if ("view".equals(action)) viewLog(req, resp);
            else if ("download".equals(action)) downloadLog(req, resp);
            else listLogsHtml(req, resp);
        } catch (Exception e) {
            log.error("Error processing request", e);
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(req, resp);
        }
    }

    private void listLogsHtml(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String logDir = logDir();
        List<LogFile> logFiles = loadLogFiles();
        req.setAttribute("logDir", logDir);
        req.setAttribute("logFiles", logFiles);
        req.getRequestDispatcher("/WEB-INF/jsp/list-logs.jsp").forward(req, resp);
    }

    /**
     * REST JSON: list log files (newest first), including rotated catalina.*.log.
     * {@code GET /logs?action=list}
     */
    private void listLogsJson(HttpServletResponse resp) throws ServletException, IOException {
        List<LogFile> logFiles = loadLogFiles();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write("{\"logDir\":\"");
            writer.write(escapeJson(logDir()));
            writer.write("\",\"files\":[");
            for (int i = 0; i < logFiles.size(); i++) {
                if (i > 0) {
                    writer.write(',');
                }
                LogFile f = logFiles.get(i);
                writer.write("{\"name\":\"");
                writer.write(escapeJson(f.getName()));
                writer.write("\",\"size\":\"");
                writer.write(escapeJson(f.getSize()));
                writer.write("\",\"lastModified\":\"");
                writer.write(escapeJson(f.getLastModified()));
                writer.write("\"}");
            }
            writer.write("]}");
        }
    }

    private List<LogFile> loadLogFiles() throws ServletException {
        String logDir = logDir();
        File dir = new File(logDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ServletException("Log directory not found: " + logDir);
        }
        return Arrays.stream(Objects.requireNonNull(dir.listFiles(File::isFile)))
                .map(this::createLogFile)
                .sorted((a, b) -> b.getLastModified().compareTo(a.getLastModified()))
                .collect(Collectors.toList());
    }

    private void viewLog(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = validateFileName(req.getParameter("file"));
        if (fileName.toLowerCase().endsWith(".gz")) {
            throw new ServletException("Compressed log files cannot be viewed; download the gunzipped file instead.");
        }
        File file = new File(logDir(), fileName);
        long totalLines = countLinesWithFallback(file);

        // Default to last page if no page specified
        int pageNum = getPageNumber(req);
        if (pageNum == 0) {
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
        File file = new File(logDir(), fileName);

        boolean isGz = fileName.toLowerCase().endsWith(".gz");

        resp.setContentType("application/octet-stream");
        String downloadName = isGz ? fileName.substring(0, fileName.length() - 3) : fileName;
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");

        if (!isGz) {
            resp.setContentLength((int) file.length());
        }

        try (InputStream in = FileUtils.openInputStream(file)) {
            if (isGz) {
                try (GZIPInputStream gzIn = new GZIPInputStream(in)) {
                    IOUtils.copy(gzIn, resp.getOutputStream());
                }
            } else {
                IOUtils.copy(in, resp.getOutputStream());
            }
        }
    }

    /**
     * REST JSON: last N lines of a log file (default {@code catalina.out}).
     * {@code GET /logs?action=tail&lines=200&file=catalina.out}
     */
    private void tailLogJson(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestedFile = req.getParameter("file");
        if (requestedFile == null || requestedFile.isEmpty()) {
            requestedFile = DEFAULT_TAIL_FILE;
        }
        String fileName = validateFileName(requestedFile);
        File file = new File(logDir(), fileName);
        int lines = getTailLineCount(req);
        List<String> lastLines = readLastLines(file, lines);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write('{');
            writer.write("\"file\":\"");
            writer.write(escapeJson(fileName));
            writer.write("\",\"returnedLines\":");
            writer.write(Integer.toString(lastLines.size()));
            writer.write(",\"fileSize\":");
            writer.write(Long.toString(file.length()));
            writer.write(",\"lines\":[");
            for (int i = 0; i < lastLines.size(); i++) {
                if (i > 0) {
                    writer.write(',');
                }
                writer.write('"');
                writer.write(escapeJson(lastLines.get(i)));
                writer.write('"');
            }
            writer.write("]}");
        }
    }

    private void writeJsonError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write("{\"error\":\"");
            writer.write(escapeJson(message == null ? "unknown error" : message));
            writer.write("\"}");
        }
    }

    private String validateFileName(String fileName) throws ServletException, IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new ServletException("File name not provided");
        }

        File requestedFile = new File(logDir(), fileName);
        if (!requestedFile.getCanonicalPath().startsWith(new File(logDir()).getCanonicalPath())) {
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

    private List<String> readLastLines(File file, int lineCount) throws IOException {
        if (lineCount <= 0 || file.length() == 0) {
            return Collections.emptyList();
        }
        List<String> lines = new ArrayList<>(lineCount);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long pos = raf.length() - 1;
            ByteArrayBuilder builder = new ByteArrayBuilder();
            int found = 0;
            while (pos >= 0 && found < lineCount) {
                raf.seek(pos);
                int b = raf.read();
                if (b == '\n') {
                    // Ignore the empty segment from a trailing newline at EOF only.
                    if (builder.length() > 0 || found > 0) {
                        lines.add(decodeLine(builder.toByteArray()));
                        builder.reset();
                        found++;
                    }
                } else if (b != '\r') {
                    builder.prepend((byte) b);
                }
                pos--;
            }
            if (builder.length() > 0 && found < lineCount) {
                lines.add(decodeLine(builder.toByteArray()));
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    private String decodeLine(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
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
        String raw = req.getParameter("page");
        if (raw == null || raw.isEmpty()) {
            return 0; // unspecified — will resolve to last page
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int getTailLineCount(HttpServletRequest req) {
        String raw = req.getParameter("lines");
        if (raw == null || raw.isEmpty()) {
            return DEFAULT_TAIL_LINES;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value < 1) {
                return DEFAULT_TAIL_LINES;
            }
            return Math.min(value, MAX_TAIL_LINES);
        } catch (NumberFormatException e) {
            return DEFAULT_TAIL_LINES;
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

    static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Growing buffer that supports prepending bytes while scanning a file backwards. */
    private static final class ByteArrayBuilder {
        private byte[] buf = new byte[256];
        private int start = 256;
        private int end = 256;

        void prepend(byte b) {
            if (start == 0) {
                int len = length();
                byte[] grown = new byte[Math.max(256, len * 2)];
                int newStart = grown.length - len;
                System.arraycopy(buf, start, grown, newStart, len);
                buf = grown;
                start = newStart;
                end = grown.length;
            }
            buf[--start] = b;
        }

        int length() {
            return end - start;
        }

        byte[] toByteArray() {
            byte[] out = new byte[length()];
            System.arraycopy(buf, start, out, 0, out.length);
            return out;
        }

        void reset() {
            start = buf.length;
            end = buf.length;
        }
    }

    @Data
    public static class LogFile {
        private String name;
        private String size;
        private String lastModified;
        private String path;
    }
}
