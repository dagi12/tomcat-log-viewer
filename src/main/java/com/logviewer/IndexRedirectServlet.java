package com.logviewer;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple redirect servlet to handle the root path
 * and index.jsp requests
 */
@WebServlet("/index.jsp")
public class IndexRedirectServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Redirect to the main logs page
        response.sendRedirect(request.getContextPath() + "/logs");
    }
}
