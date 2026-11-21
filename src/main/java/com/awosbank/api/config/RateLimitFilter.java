package com.awosbank.api.config;

import com.awosbank.api.service.RateLimitService;
import com.awosbank.api.service.RateLimitService.Decisao;
import com.awosbank.api.service.RateLimitService.Grupo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Limita requisições por IP em {@code POST /login}, {@code POST /cadastro} e {@code /api/**}.
 *
 * <p>Fica DENTRO da cadeia do Spring Security (antes do filtro de login): como um filtro de servlet comum
 * roda depois dela, ele nunca veria o {@code POST /login}, que a própria cadeia processa e encerra.
 * Por isso não é um {@code @Component} (seria registrado duas vezes) — é criado em {@link SecurityConfig}.
 *
 * <p>O IP vem de {@code getRemoteAddr()}, o mesmo usado na auditoria; atrás de proxy reverso configure
 * {@code FORWARD_HEADERS_STRATEGY=framework} para enxergar o IP real do cliente.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimit;

    public RateLimitFilter(RateLimitService rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Grupo grupo = rateLimit.habilitado() ? classificar(request) : null;
        if (grupo == null) {
            chain.doFilter(request, response);
            return;
        }

        Decisao decisao = rateLimit.verificar(grupo, request.getRemoteAddr());
        if (decisao.permitido()) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit excedido (grupo={}, ip={}, caminho={})", grupo, request.getRemoteAddr(), caminho(request));
        response.setHeader("Retry-After", String.valueOf(decisao.retryAfterSegundos()));
        recusar(request, response, grupo, decisao.retryAfterSegundos());
    }

    private void recusar(HttpServletRequest request, HttpServletResponse response, Grupo grupo,
                         long segundos) throws IOException {
        if (grupo == Grupo.API) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"ok\":false,\"mensagem\":\"Muitas requisições. Tente novamente em "
                    + segundos + " segundo(s).\"}");
            return;
        }
        // Formulários web: volta para a própria tela com um aviso (sem HTTP 429 cru na cara do cliente).
        String destino = "/cadastro".equals(caminho(request)) ? "/cadastro?limite" : "/login?limite";
        response.sendRedirect(request.getContextPath() + destino);
    }

    private Grupo classificar(HttpServletRequest request) {
        String caminho = caminho(request);
        if ("POST".equalsIgnoreCase(request.getMethod())
                && ("/login".equals(caminho) || "/cadastro".equals(caminho))) {
            return Grupo.AUTENTICACAO;
        }
        if (caminho.startsWith("/api/")) return Grupo.API;
        return null;
    }

    private String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        return (contexto != null && !contexto.isEmpty() && uri.startsWith(contexto))
                ? uri.substring(contexto.length()) : uri;
    }
}
