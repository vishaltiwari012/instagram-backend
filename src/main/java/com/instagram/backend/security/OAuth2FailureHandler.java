package com.instagram.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        log.error("❌ OAuth2 authentication failed for URI [{}]: {}", request.getRequestURI(), exception.getMessage(), exception);

        // Optionally set response status
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Redirect to frontend error page
        response.sendRedirect("http://localhost:4200/error.html");
    }
}
