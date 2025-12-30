package pl.edu.amu.wmi.logviewer;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogViewerServletIT {
    private static final String TEST_LOG_DIR = "target/test-logs";
    private static final String TEST_FILE = "test.log";
    private static final String TEST_CONTENT = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5";

    private static Tomcat tomcat;
    private LogViewerServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ByteArrayOutputStream responseOutput;

    @BeforeAll
    static void setup() throws Exception {
        // Create test log directory and file
        File logDir = new File(TEST_LOG_DIR);
        FileUtils.deleteDirectory(logDir);
        logDir.mkdirs();

        // Create test log file
        File testFile = new File(logDir, TEST_FILE);
        FileUtils.writeStringToFile(testFile, TEST_CONTENT, StandardCharsets.UTF_8.toString());

        // Start embedded Tomcat
        tomcat = new Tomcat();
        tomcat.setPort(0); // Auto-select port
        tomcat.getConnector();

        // Create webapp context
        StandardContext ctx = (StandardContext) tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        ctx.setResources(new StandardRoot(ctx));
        ctx.addApplicationListener(FixContextListener.class.getName());

        // Set log directory as system property
        System.setProperty("catalina.base", logDir.getAbsolutePath());

        // Start server
        tomcat.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
        FileUtils.deleteDirectory(new File(TEST_LOG_DIR));
    }

    @BeforeEach
    void setUp() {
        servlet = new LogViewerServlet();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        responseOutput = new ByteArrayOutputStream();
    }

    @Test
    void testListLogs() throws Exception {
        // When
        when(request.getParameter("action")).thenReturn(null);
        when(response.getWriter()).thenReturn(new PrintWriter(responseOutput));

        servlet.doGet(request, response);

        // Then
        verify(response).setContentType("text/html");
        String output = responseOutput.toString();
        assertThat(output).contains("Log Files");
        assertThat(output).contains(TEST_FILE);
    }

    @Test
    void testViewLog() throws Exception {
        // When
        when(request.getParameter("action")).thenReturn("view");
        when(request.getParameter("file")).thenReturn(TEST_FILE);
        when(request.getParameter("page")).thenReturn("1");
        when(response.getWriter()).thenReturn(new PrintWriter(responseOutput));

        servlet.doGet(request, response);

        // Then
        verify(response).setContentType("text/html");
        String output = responseOutput.toString();
        assertThat(output).contains("View Log - " + TEST_FILE);
        assertThat(output).contains("Line 1");
    }

    @Test
    void testDownloadLog() throws Exception {
        // Given
        when(request.getParameter("action")).thenReturn("download");
        when(request.getParameter("file")).thenReturn(TEST_FILE);

        // Create a mock ServletOutputStream
        ServletOutputStream outputStream = new ServletOutputStream() {
            @Override
            public void write(int b) {
                // No-op for testing
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(jakarta.servlet.WriteListener writeListener) {
                // No-op for testing
            }
        };

        when(response.getOutputStream()).thenReturn(outputStream);

        // When
        servlet.doGet(request, response);

        // Then
        verify(response).setContentType("application/octet-stream");
        verify(response).setHeader("Content-Disposition", "attachment; filename=\"" + TEST_FILE + "\"");
    }

}
