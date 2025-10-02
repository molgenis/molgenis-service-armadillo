package org.molgenis.armadillo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class AuthenticationFilter extends GenericFilterBean {

    private String AUTH_TOKEN;

    public void setAuthToken(String authToken) {
        AUTH_TOKEN = authToken;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                Enumeration<String> headers = httpRequest.getHeaderNames();
                String URI = httpRequest.getRequestURI();

                if (URI.startsWith("/actuator/")
                        && !URI.equals("/actuator/info")
                        && !URI.equals("/actuator/health")) {
                    Authentication authentication =
                            AuthenticationService.getAuthentication((HttpServletRequest) request, AUTH_TOKEN);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                HttpSession session = httpRequest.getSession(true);
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                filterChain.doFilter(request, response);
            }
        } catch (Exception exp) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            this.logger.error(exp.getMessage(), exp);
        }
    }
}