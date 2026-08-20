package pl.edu.amu.wmi.logviewer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogViewerServletTailTest {

    private File catalinaBase;
    private String previousCatalinaBase;
    private LogViewerServlet servlet;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ByteArrayOutputStream responseOutput;

    @BeforeEach
    void setUp() throws Exception {
        previousCatalinaBase = System.getProperty("catalina.base");
        catalinaBase = Files.createTempDirectory("log-viewer-test").toFile();
        File logsDir = new File(catalinaBase, "logs");
        assertThat(logsDir.mkdirs()).isTrue();
        System.setProperty("catalina.base", catalinaBase.getAbsolutePath());

        FileUtils.writeStringToFile(
                new File(logsDir, "catalina.out"),
                "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n",
                "UTF-8");

        servlet = new LogViewerServlet();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        responseOutput = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(responseOutput, true));
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
    void tailDefaultsToCatalinaOutLastLines() throws Exception {
        when(request.getParameter("action")).thenReturn("tail");
        when(request.getParameter("file")).thenReturn(null);
        when(request.getParameter("lines")).thenReturn("3");

        servlet.doGet(request, response);

        verify(response).setContentType("application/json;charset=UTF-8");
        String json = responseOutput.toString(StandardCharsets.UTF_8);
        assertThat(json).contains("\"file\":\"catalina.out\"");
        assertThat(json).contains("\"returnedLines\":3");
        assertThat(json).contains("\"Line 3\"");
        assertThat(json).contains("\"Line 4\"");
        assertThat(json).contains("\"Line 5\"");
        assertThat(json).doesNotContain("\"Line 1\"");
    }

    @Test
    void escapeJsonEscapesQuotesAndControls() {
        assertThat(LogViewerServlet.escapeJson("a\"b\\c\n")).isEqualTo("a\\\"b\\\\c\\n");
    }
}
