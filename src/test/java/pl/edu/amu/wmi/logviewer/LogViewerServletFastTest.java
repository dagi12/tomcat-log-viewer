package pl.edu.amu.wmi.logviewer;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("fast")
class LogViewerServletFastTest {

    private File catalinaBase;
    private File logsDir;
    private String previousCatalinaBase;
    private LogViewerServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ByteArrayOutputStream responseBody;
    private RequestDispatcher dispatcher;

    private static void writeGzip(File target, String content) throws Exception {
        try (GZIPOutputStream gz = new GZIPOutputStream(new FileOutputStream(target))) {
            gz.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        previousCatalinaBase = System.getProperty("catalina.base");
        catalinaBase = Files.createTempDirectory("log-viewer-fast").toFile();
        logsDir = new File(catalinaBase, "logs");
        assertThat(logsDir.mkdirs()).isTrue();
        System.setProperty("catalina.base", catalinaBase.getAbsolutePath());

        FileUtils.writeStringToFile(new File(logsDir, "catalina.out"),
                                    "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n", "UTF-8");
        FileUtils.writeStringToFile(new File(logsDir, "app.log"), "app\n", "UTF-8");
        FileUtils.writeStringToFile(new File(logsDir, "noext"), "raw\n", "UTF-8");
        new File(logsDir, "subdir").mkdir();

        writeGzip(new File(logsDir, "rotated.log.gz"), "gzipped line 1\ngzipped line 2\n");

        servlet = new LogViewerServlet();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        dispatcher = Mockito.mock(RequestDispatcher.class);
        responseBody = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody, true));
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (previousCatalinaBase == null) {
            System.clearProperty("catalina.base");
        } else {
            System.setProperty("catalina.base", previousCatalinaBase);
        }
        FileUtils.deleteDirectory(catalinaBase);
    }

    @Test
    void listJsonIncludesAllFilesAndExcludesDirectories() throws Exception {
        when(request.getParameter("action")).thenReturn("list");

        servlet.doGet(request, response);

        String json = responseBody.toString(StandardCharsets.UTF_8);
        assertThat(json).contains("\"name\":\"catalina.out\"");
        assertThat(json).contains("\"name\":\"app.log\"");
        assertThat(json).contains("\"name\":\"noext\"");
        assertThat(json).contains("\"name\":\"rotated.log.gz\"");
        assertThat(json).doesNotContain("\"name\":\"subdir\"");
    }

    @Test
    void downloadPlainFileStreamsBytes() throws Exception {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        when(request.getParameter("action")).thenReturn("download");
        when(request.getParameter("file")).thenReturn("app.log");
        when(response.getOutputStream()).thenReturn(out);

        servlet.doGet(request, response);

        verify(response).setContentType("application/octet-stream");
        verify(response).setHeader("Content-Disposition", "attachment; filename=\"app.log\"");
        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("app\n");
    }

    @Test
    void downloadGzDecompressesAndStripsExtension() throws Exception {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        when(request.getParameter("action")).thenReturn("download");
        when(request.getParameter("file")).thenReturn("rotated.log.gz");
        when(response.getOutputStream()).thenReturn(out);

        servlet.doGet(request, response);

        verify(response).setHeader("Content-Disposition", "attachment; filename=\"rotated.log\"");
        verify(response, never()).setContentLength(Mockito.anyInt());
        assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("gzipped line 1\ngzipped line 2\n");
    }

    @Test
    void viewGzForwardsToErrorPage() throws Exception {
        when(request.getParameter("action")).thenReturn("view");
        when(request.getParameter("file")).thenReturn("rotated.log.gz");

        servlet.doGet(request, response);

        ArgumentCaptor<String> error = ArgumentCaptor.forClass(String.class);
        verify(request).setAttribute(Mockito.eq("error"), error.capture());
        assertThat(error.getValue()).contains("Compressed log files");
        verify(request).getRequestDispatcher("/WEB-INF/jsp/error.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void viewWithoutPageOpensLastPage() throws Exception {
        StringBuilder many = new StringBuilder();
        for (int i = 1; i <= 2005; i++) {
            many.append("L").append(i).append('\n');
        }
        FileUtils.writeStringToFile(new File(logsDir, "big.log"), many.toString(), "UTF-8");

        when(request.getParameter("action")).thenReturn("view");
        when(request.getParameter("file")).thenReturn("big.log");
        when(request.getParameter("page")).thenReturn(null);

        servlet.doGet(request, response);

        verify(request).setAttribute("currentPage", 2);
        verify(request).setAttribute("totalPages", 2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> lines = ArgumentCaptor.forClass(List.class);
        verify(request).setAttribute(Mockito.eq("logContent"), lines.capture());
        assertThat(lines.getValue()).contains("L2005");
        assertThat(lines.getValue()).doesNotContain("L1");
        verify(request).getRequestDispatcher("/WEB-INF/jsp/view-log.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void viewWithExplicitPageOneReturnsFirstChunk() throws Exception {
        when(request.getParameter("action")).thenReturn("view");
        when(request.getParameter("file")).thenReturn("catalina.out");
        when(request.getParameter("page")).thenReturn("1");

        servlet.doGet(request, response);

        verify(request).setAttribute("currentPage", 1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> lines = ArgumentCaptor.forClass(List.class);
        verify(request).setAttribute(Mockito.eq("logContent"), lines.capture());
        assertThat(lines.getValue()).containsExactly("Line 1", "Line 2", "Line 3", "Line 4", "Line 5");
    }

    @Test
    void listHtmlForwardsWithLogFiles() throws Exception {
        when(request.getParameter("action")).thenReturn(null);

        servlet.doGet(request, response);

        verify(request).setAttribute(Mockito.eq("logDir"), anyString());
        verify(request).setAttribute(Mockito.eq("logFiles"), Mockito.any());
        verify(request).getRequestDispatcher("/WEB-INF/jsp/list-logs.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void escapeJsonEscapesControls() {
        assertThat(LogViewerServlet.escapeJson("a\"b\\c\n\r\t")).isEqualTo("a\\\"b\\\\c\\n\\r\\t");
    }

    private static final class CapturingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            buf.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // no-op
        }

        String toString(java.nio.charset.Charset charset) {
            return buf.toString(charset);
        }
    }
}
