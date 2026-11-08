package com.awosbank.api.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.awosbank.api.dto.RespostaApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class TratamentoDeErrosTest {

    private static final String SEGREDO = "jdbc:postgresql://db-interno:5432/awos?user=app&password=segredo";

    /** Exceção de domínio com status próprio: não é "erro inesperado". */
    @ResponseStatus(HttpStatus.CONFLICT)
    static class ConflitoException extends RuntimeException {}

    private final GlobalExceptionHandler web = new GlobalExceptionHandler();
    private final ApiExceptionHandler api = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/transferencia");

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

    // ---------- páginas web ----------

    @Test
    void webNaoMostraMensagemInternaAoUsuarioERegistraOErroNoLog() throws Exception {
        ConcurrentModel model = new ConcurrentModel();
        IllegalStateException erro = new IllegalStateException(SEGREDO);

        String view = web.handleGeneric(erro, request, model);

        assertEquals("error", view);
        assertEquals("Erro inesperado", model.getAttribute("titulo"));
        assertEquals(GlobalExceptionHandler.MENSAGEM_GENERICA, model.getAttribute("mensagem"));
        assertFalse(model.asMap().values().toString().contains("segredo"), "nada interno pode ir para a página");

        String codigo = (String) model.getAttribute("codigoErro");
        assertNotNull(codigo);
        assertEquals(8, codigo.length());

        assertEquals(1, logs.list.size());
        ILoggingEvent evento = logs.list.get(0);
        assertEquals(Level.ERROR, evento.getLevel());
        assertTrue(evento.getFormattedMessage().contains("codigo=" + codigo), "o log leva o mesmo código mostrado ao usuário");
        assertTrue(evento.getFormattedMessage().contains("POST /transferencia"));
        assertNotNull(evento.getThrowableProxy(), "o stack trace tem de ir para o log");
        assertEquals(SEGREDO, evento.getThrowableProxy().getMessage());
    }

    @Test
    void webGeraUmCodigoDiferenteACadaOcorrencia() throws Exception {
        ConcurrentModel m1 = new ConcurrentModel();
        ConcurrentModel m2 = new ConcurrentModel();

        web.handleGeneric(new RuntimeException("a"), request, m1);
        web.handleGeneric(new RuntimeException("b"), request, m2);

        assertNotEquals(m1.getAttribute("codigoErro"), m2.getAttribute("codigoErro"));
    }

    @Test
    void webContinuaMostrandoAMensagemDeNegocio() {
        ConcurrentModel model = new ConcurrentModel();

        String view = web.handleBusiness(new BusinessException("Saldo insuficiente"), model);

        assertEquals("error", view);
        assertEquals("Saldo insuficiente", model.getAttribute("mensagem"));
        assertTrue(logs.list.isEmpty(), "erro de negócio é esperado e não polui o log de erros");
    }

    @Test
    void webRepassaErrosQueJaTemTratamentoProprio() {
        Exception[] repassadas = {
                new MissingServletRequestParameterException("pagina", "int"),   // 400 do MVC (ErrorResponse)
                new TypeMismatchException("abc", Integer.class),                // 400
                new AccessDeniedException("negado"),                            // Spring Security
                new BadCredentialsException("sessão inválida"),                 // Spring Security
                new ConflitoException()                                         // @ResponseStatus
        };
        for (Exception original : repassadas) {
            Exception lancada = assertThrows(Exception.class,
                    () -> web.handleGeneric(original, request, new ConcurrentModel()));
            assertSame(original, lancada, original.getClass().getSimpleName() + " deve ser relançada intacta");
        }
        assertTrue(logs.list.isEmpty(), "nenhuma delas é erro inesperado");
    }

    // ---------- API REST ----------

    @Test
    void apiResponde500EmJsonGenericoComCodigoENaoVazaDetalhes() throws Exception {
        request.setRequestURI("/api/deposito");

        ResponseEntity<RespostaApi> resposta = api.erroInesperado(new IllegalStateException(SEGREDO), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
        RespostaApi corpo = resposta.getBody();
        assertNotNull(corpo);
        assertFalse(corpo.ok());
        assertFalse(corpo.mensagem().contains("segredo"));
        assertFalse(corpo.mensagem().contains("jdbc"));

        assertEquals(1, logs.list.size());
        ILoggingEvent evento = logs.list.get(0);
        assertEquals(Level.ERROR, evento.getLevel());
        assertNotNull(evento.getThrowableProxy());
        String codigo = evento.getFormattedMessage().replaceAll(".*codigo=([0-9A-F]{8}).*", "$1");
        assertTrue(corpo.mensagem().contains(codigo), "a resposta traz o mesmo código que está no log");
    }

    @Test
    void apiRepassaErrosQueJaTemTratamentoProprio() {
        Exception[] repassadas = {
                new MissingServletRequestParameterException("pagina", "int"),
                new TypeMismatchException("abc", Integer.class),
                new HttpMessageNotReadableException("corpo", new MockHttpInputMessage(new byte[0])),
                new AccessDeniedException("negado"),
                new ConflitoException()
        };
        for (Exception original : repassadas) {
            Exception lancada = assertThrows(Exception.class, () -> api.erroInesperado(original, request));
            assertSame(original, lancada);
        }
        assertTrue(logs.list.isEmpty());
    }
}
