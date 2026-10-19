package com.bgssai.shortai.configuration;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Replaces the Whitelabel error page. Browser document requests get the SPA shell;
 * everyone else gets the JSON envelope.
 */
@Controller
public class SpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (response.isCommitted()) {
            return;
        }
        if (SpaDocumentRequests.shouldServeIndex(request)) {
            response.setStatus(HttpServletResponse.SC_OK);
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr instanceof Integer value ? value : 500;
        String code = status == 404 ? "404000" : "500000";
        String message = status == 404 ? "resource not found" : "system error";
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"success\":false,\"result\":null}");
    }
}
