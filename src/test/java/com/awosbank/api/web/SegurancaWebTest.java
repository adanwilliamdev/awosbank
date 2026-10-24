package com.awosbank.api.web;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaWebTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;

    @Test
    void rotasProtegidasRedirecionamParaLogin() throws Exception {
        for (String rota : new String[]{"/dashboard", "/extrato", "/pix", "/transferencia", "/perfil"}) {
            mvc.perform(get(rota))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    @Test
    void consoleH2NaoExisteMais() throws Exception {
        mvc.perform(get("/h2-console/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void apiSemCsrfEhRejeitada() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        mvc.perform(post("/api/pix/keys").with(user(u.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMAIL\",\"chave\":\"a@b.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chavePixComPayloadXssEhRecusadaPelaApi() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        mvc.perform(post("/api/pix/keys").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMAIL\",\"chave\":\"<img src=x onerror=alert(1)>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void chavePixEmailValidaEhCadastradaEmMinusculas() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        mvc.perform(post("/api/pix/keys").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"EMAIL\",\"chave\":\"Pessoa@Exemplo.COM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void trocarEmailEncerraASessaoEPedeNovoLogin() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        mvc.perform(post("/perfil").with(user(u.getEmail())).with(csrf())
                        .param("nomeCompleto", "Nome Atualizado")
                        .param("email", "Novo.Email" + u.getId() + "@awosbank.test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?emailAlterado"));
    }

    @Test
    void loginIgnoraMaiusculasNoEmail() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        mvc.perform(post("/login").with(csrf())
                        .param("username", u.getEmail().toUpperCase())
                        .param("password", "senha123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
