package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Rate limit por IP de ponta a ponta. O perfil "test" o desliga; aqui ele é ligado com limites pequenos.
 * Cada teste usa o seu próprio IP, porque a contagem vive no contexto Spring (compartilhado entre os testes).
 */
@SpringBootTest(properties = {
        "awosbank.seguranca.rate-limit.habilitado=true",
        "awosbank.seguranca.rate-limit.max-autenticacao=3",
        "awosbank.seguranca.rate-limit.max-api=5",
        "awosbank.seguranca.rate-limit.janela-segundos=60"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }

    @Test
    void loginAposOLimiteVoltaParaATelaComAvisoENaoTentaAutenticar() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/login").with(ip("203.0.113.1")).with(csrf())
                            .param("username", u.getEmail()).param("password", "errada"))
                    .andExpect(redirectedUrl("/login?error"));
        }

        // 4ª tentativa: nem a senha CERTA passa — a requisição nem chega ao login
        mvc.perform(post("/login").with(ip("203.0.113.1")).with(csrf())
                        .param("username", u.getEmail()).param("password", SENHA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?limite"))
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void cadastroAposOLimiteVoltaParaOCadastroComAviso() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/cadastro").with(ip("203.0.113.2")).with(csrf())); // inválido de propósito: só conta
        }

        mvc.perform(post("/cadastro").with(ip("203.0.113.2")).with(csrf()))
                .andExpect(redirectedUrl("/cadastro?limite"));
    }

    @Test
    void outroIpNaoEhAfetado() throws Exception {
        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/login").with(ip("203.0.113.3")).with(csrf())
                    .param("username", "x@y.com").param("password", "errada"));
        }

        mvc.perform(post("/login").with(ip("203.0.113.4")).with(csrf())
                        .param("username", "x@y.com").param("password", "errada"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void paginaDeLoginEmGetNaoContaNemEhBloqueada() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/login").with(ip("203.0.113.5"))).andExpect(status().isOk());
        }
    }

    @Test
    void apiAposOLimiteResponde429ComJsonERetryAfter() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/auditoria").with(ip("203.0.113.6")).with(user(u.getEmail())))
                    .andExpect(status().isOk());
        }

        mvc.perform(get("/api/auditoria").with(ip("203.0.113.6")).with(user(u.getEmail())))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.mensagem").exists());
    }
}
