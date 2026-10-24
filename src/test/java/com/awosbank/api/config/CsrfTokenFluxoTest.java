package com.awosbank.api.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão do 403 ao cadastrar chave PIX: o app.js devolve no header X-XSRF-TOKEN o valor CRU do
 * cookie XSRF-TOKEN. O handler padrão (Xor) espera o valor mascarado; o simples aceita o cru.
 */
class CsrfTokenFluxoTest {

    private final CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();

    private MockHttpServletRequest requisicaoDoNavegador(String tokenCru) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("XSRF-TOKEN", tokenCru));
        req.addHeader("X-XSRF-TOKEN", tokenCru); // exatamente o que o app.js envia
        return req;
    }

    @Test
    void handlerSimplesAceitaOTokenCruDoCookieEnviadoNoHeader() {
        MockHttpServletRequest req = requisicaoDoNavegador("token-de-teste");
        CsrfToken token = repo.loadToken(req);

        String enviado = new CsrfTokenRequestAttributeHandler().resolveCsrfTokenValue(req, token);

        assertThat(enviado).isEqualTo(token.getToken());
    }

    @Test
    void handlerPadraoXorNaoReconheceOTokenCru_causaDoErro403Original() {
        MockHttpServletRequest req = requisicaoDoNavegador("token-de-teste");
        CsrfToken token = repo.loadToken(req);

        String resolvido = new XorCsrfTokenRequestAttributeHandler().resolveCsrfTokenValue(req, token);

        assertThat(resolvido).isNotEqualTo(token.getToken());
    }

    @Test
    void cookieDeTokenEGravadoSemHttpOnlyParaOJavaScriptLer() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        repo.saveToken(repo.generateToken(req), req, res);

        assertThat(res.getCookie("XSRF-TOKEN")).isNotNull();
        assertThat(res.getCookie("XSRF-TOKEN").isHttpOnly()).isFalse();
    }
}
