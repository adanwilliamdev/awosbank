package com.awosbank.api.web;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.AuditoriaService;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tela web "Atividade da conta" (GET /auditoria). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaWebTest {

    private static final String SENHA = "senha123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @Autowired AuditoriaService auditoriaService;

    @Test
    void semLoginRedirecionaParaOLogin() throws Exception {
        mvc.perform(get("/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void mostraOHistoricoDoProprioUsuario() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA); // o cadastro já fica registrado

        mvc.perform(get("/auditoria").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria"))
                .andExpect(content().string(containsString("Atividade da conta")))
                .andExpect(content().string(containsString("Cadastro de cliente")));
    }

    @Test
    void naoMostraORegistroDeOutroUsuario() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        auditoriaService.registrar(AcaoAuditoria.SAQUE, b.getId(), b.getEmail(), false, "motivo=so-do-b");

        mvc.perform(get("/auditoria").with(user(a.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("so-do-b"))));
    }

    @Test
    void filtroDeFalhasListaSoAsFalhas() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);
        auditoriaService.registrar(AcaoAuditoria.TRANSFERENCIA, u.getId(), u.getEmail(), false, "motivo=Saldo insuficiente");

        mvc.perform(get("/auditoria").param("resultado", "falhas").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", "falhas"))
                .andExpect(content().string(containsString("Saldo insuficiente")))
                .andExpect(content().string(not(containsString("Cadastro de cliente"))));
    }

    @Test
    void estadoVazioQuandoNaoHaFalhas() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(get("/auditoria").param("resultado", "falhas").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhuma falha registrada")));
    }

    @Test
    void paginaNegativaOuLixoNaoQuebra() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        mvc.perform(get("/auditoria").param("pagina", "-5").param("resultado", "qualquer").with(user(u.getEmail())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resultado", "todos"));
    }
}
