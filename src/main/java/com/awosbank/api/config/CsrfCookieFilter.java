package com.awosbank.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * No Spring Security 6 o token CSRF é "deferred": o cookie XSRF-TOKEN só é gravado quando
 * alguém lê o token. Este filtro o lê em toda requisição para que o cookie sempre exista
 * quando o JavaScript (app.js) precisar dele.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken(); // força a gravação do cookie
        }
        chain.doFilter(request, response);
    }
}
