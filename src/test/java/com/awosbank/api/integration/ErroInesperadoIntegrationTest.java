package com.awosbank.api.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.exception.ApiExceptionHandler;
import com.awosbank.api.exception.GlobalExceptionHandler;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Erros inesperados de ponta a ponta: o cliente recebe uma resposta genérica (com código de ocorrência)
 * e o detalhe interno fica só no log.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErroInesperadoIntegrationTest {

    private static final String SEGREDO = "jdbc:postgresql://db-interno:5432/awos?password=segredo";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @MockBean TransacaoService transacaoService;

    private ListAppender<ILoggingEvent> logs;
    private Logger loggerWeb;
    private Logger loggerApi;

    @BeforeEach
    void capturarLogs() {
        logs = new ListAppender<>();
        logs.start();
        loggerWeb = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        loggerApi = (Logger) LoggerFactory.getLogger(ApiExceptionHandler.class);
        loggerWeb.addAppender(logs);
        loggerApi.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        loggerWeb.detachAppender(logs);
        loggerApi.detachAppender(logs);
    }

    @Test
    void apiResponde500EmJsonSemVazarDetalhesEComCodigoNoLog() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        when(transacaoService.depositar(any(), any())).thenThrow(new IllegalStateException(SEGREDO));

        MvcResult resultado = mvc.perform(post("/api/deposito").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\": 10.00}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ok").value(false))
                .andReturn();

        // O JSON sai em UTF-8 sem "charset" no Content-Type; sem informar o charset o MockMvc decodifica como
        // ISO-8859-1 ("código" vira "cÃ³digo") e a regex abaixo não encontra o código da ocorrência.
        String corpo = resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(corpo.contains("segredo") || corpo.contains("jdbc"), "o detalhe interno não pode sair na resposta");

        String mensagem = JsonPath.read(corpo, "$.mensagem");
        String codigo = mensagem.replaceAll(".*código ([0-9A-F]{8})\\..*", "$1");
        assertEquals(1, logs.list.size());
        ILoggingEvent evento = logs.list.get(0);
        assertEquals(Level.ERROR, evento.getLevel());
        assertTrue(evento.getFormattedMessage().contains("codigo=" + codigo));
        assertTrue(evento.getFormattedMessage().contains("/api/deposito"));
        assertEquals(SEGREDO, evento.getThrowableProxy().getMessage(), "o detalhe fica no log");
    }

    @Test
    void paginaWebResponde500ComMensagemGenericaECodigoSemVazarDetalhes() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");
        when(transacaoService.transferir(any(), any())).thenThrow(new IllegalStateException(SEGREDO));

        MvcResult resultado = mvc.perform(post("/transferencia").with(user(u.getEmail())).with(csrf())
                        .param("numeroContaDestino", "111111-1").param("valor", "10.00")
                        .param("tipo", "PIX").param("senha", "senha123"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("titulo", "Erro inesperado"))
                .andExpect(model().attributeExists("codigoErro"))
                .andReturn();

        String html = resultado.getResponse().getContentAsString();
        assertFalse(html.contains("segredo") || html.contains("jdbc"), "o detalhe interno não pode aparecer na página");
        String codigo = (String) resultado.getModelAndView().getModel().get("codigoErro");
        assertTrue(html.contains(codigo), "a página mostra o código para o usuário informar ao suporte");

        assertEquals(1, logs.list.size());
        assertTrue(logs.list.get(0).getFormattedMessage().contains("codigo=" + codigo));
        assertNotNull(logs.list.get(0).getThrowableProxy());
    }

    @Test
    void parametroInvalidoContinuaSendo400EmVezDeVirar500() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, "senha123");

        mvc.perform(get("/api/auditoria").param("pagina", "abc").with(user(u.getEmail())))
                .andExpect(status().isBadRequest());

        assertTrue(logs.list.isEmpty(), "erro do cliente não é erro inesperado e não vai para o log de erros");
    }
}
