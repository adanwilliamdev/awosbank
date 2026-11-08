package com.awosbank.api.audit;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.AuditoriaService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class AuditoriaAspectTest {

    /** Só existe para obter uma instância real da anotação @Auditado. */
    static class Alvo {
        @Auditado(AcaoAuditoria.TRANSFERENCIA)
        void transferir() {}
    }

    private AuditoriaService auditoriaService;
    private AuditoriaAspect aspect;
    private Auditado auditado;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() throws Exception {
        auditoriaService = Mockito.mock(AuditoriaService.class);
        aspect = new AuditoriaAspect(auditoriaService);
        auditado = Alvo.class.getDeclaredMethod("transferir").getAnnotation(Auditado.class);
        pjp = Mockito.mock(ProceedingJoinPoint.class);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private TransferenciaDTO dto() {
        TransferenciaDTO d = new TransferenciaDTO();
        d.setNumeroContaDestino("222222-2");
        d.setValor(new BigDecimal("150.00"));
        d.setTipo(TipoTransacao.PIX);
        d.setSenha("SEGREDO-DO-CLIENTE");
        return d;
    }

    @Test
    void sucessoRegistraComoSucessoComComprovanteENuncaComASenha() throws Throwable {
        Usuario u = Usuario.builder().email("cliente@awosbank.com").build();
        Transacao t = Transacao.builder().comprovanteCodigo("CMP-123").build();
        when(pjp.getArgs()).thenReturn(new Object[]{u, dto()});
        when(pjp.proceed()).thenReturn(t);

        Object retorno = aspect.auditar(pjp, auditado);

        assertSame(t, retorno);
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), isNull(), eq("cliente@awosbank.com"),
                eq(true), detalhes.capture());
        assertTrue(detalhes.getValue().contains("comprovante=CMP-123"));
        assertTrue(detalhes.getValue().contains("valor=150.00"));
        assertTrue(detalhes.getValue().contains("destino=222222-2"));
        assertFalse(detalhes.getValue().contains("SEGREDO"), "a senha nunca pode ir para a auditoria");
    }

    @Test
    void falhaDeNegocioEhRegistradaComMotivoERelancada() throws Throwable {
        Usuario u = Usuario.builder().email("cliente@awosbank.com").build();
        when(pjp.getArgs()).thenReturn(new Object[]{u, dto()});
        BusinessException erro = new BusinessException("Saldo insuficiente");
        when(pjp.proceed()).thenThrow(erro);

        BusinessException lancada = assertThrows(BusinessException.class, () -> aspect.auditar(pjp, auditado));

        assertSame(erro, lancada);
        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), isNull(), eq("cliente@awosbank.com"),
                eq(false), detalhes.capture());
        assertTrue(detalhes.getValue().contains("motivo=Saldo insuficiente"));
        assertFalse(detalhes.getValue().contains("SEGREDO"));
    }

    @Test
    void erroInesperadoNaoVazaMensagemInternaParaAAuditoria() throws Throwable {
        when(pjp.getArgs()).thenReturn(new Object[]{});
        when(pjp.proceed()).thenThrow(new IllegalStateException("jdbc:postgresql://host/segredo"));

        assertThrows(IllegalStateException.class, () -> aspect.auditar(pjp, auditado));

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), isNull(), eq("anonimo"), eq(false),
                detalhes.capture());
        verify(auditoriaService, never()).buscarIdPorEmail(any());
        assertTrue(detalhes.getValue().contains("IllegalStateException"));
        assertFalse(detalhes.getValue().contains("jdbc"));
    }

    @Test
    void usuarioAutenticadoNoContextoTemPrioridadeSobreOsArgumentos() throws Throwable {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "logado@awosbank.com", "n/a", AuthorityUtils.createAuthorityList("ROLE_CLIENTE")));
        Usuario outro = Usuario.builder().email("outro@awosbank.com").build();
        when(pjp.getArgs()).thenReturn(new Object[]{outro, new BigDecimal("5.00")});
        when(pjp.proceed()).thenReturn(null);

        aspect.auditar(pjp, auditado);

        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), isNull(), eq("logado@awosbank.com"),
                eq(true), eq("valor=5.00"));
    }

    private static void autenticarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, "n/a", AuthorityUtils.createAuthorityList("ROLE_CLIENTE")));
    }

    @Test
    void usaOIdDoUsuarioRecebidoComoArgumentoSemConsultarOBanco() throws Throwable {
        UUID id = UUID.randomUUID();
        Usuario u = Usuario.builder().id(id).email("cliente@awosbank.com").build();
        when(pjp.getArgs()).thenReturn(new Object[]{u, new BigDecimal("5.00")});
        when(pjp.proceed()).thenReturn(null);

        aspect.auditar(pjp, auditado);

        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq(id), eq("cliente@awosbank.com"),
                eq(true), eq("valor=5.00"));
        verify(auditoriaService, never()).buscarIdPorEmail(any());
    }

    @Test
    void resolveOIdAntesDeExecutarParaSobreviverAoEncerramentoDaConta() throws Throwable {
        // Ex.: encerrar(email, senha) apaga o usuário; depois disso o e-mail não resolve mais para ninguém.
        UUID id = UUID.randomUUID();
        autenticarComo("cliente@awosbank.com");
        when(auditoriaService.buscarIdPorEmail("cliente@awosbank.com")).thenReturn(id);
        when(pjp.getArgs()).thenReturn(new Object[]{"cliente@awosbank.com", "senha"});
        when(pjp.proceed()).thenReturn(null);

        aspect.auditar(pjp, auditado);

        InOrder ordem = inOrder(auditoriaService, pjp);
        ordem.verify(auditoriaService).buscarIdPorEmail("cliente@awosbank.com");
        ordem.verify(pjp).proceed();
        ordem.verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq(id),
                eq("cliente@awosbank.com"), eq(true), any());
    }

    @Test
    void falhaTambemFicaVinculadaAoIdResolvidoAntes() throws Throwable {
        UUID id = UUID.randomUUID();
        autenticarComo("cliente@awosbank.com");
        when(auditoriaService.buscarIdPorEmail("cliente@awosbank.com")).thenReturn(id);
        when(pjp.getArgs()).thenReturn(new Object[]{"cliente@awosbank.com", "errada"});
        when(pjp.proceed()).thenThrow(new BusinessException("Senha incorreta"));

        assertThrows(BusinessException.class, () -> aspect.auditar(pjp, auditado));

        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq(id), eq("cliente@awosbank.com"),
                eq(false), any());
    }

    @Test
    void cadastroUsaOIdDoUsuarioCriadoRetornadoPelaOperacao() throws Throwable {
        UUID novoId = UUID.randomUUID();
        com.awosbank.api.dto.CadastroDTO cadastro = new com.awosbank.api.dto.CadastroDTO();
        cadastro.setEmail("novo@awosbank.com");
        Usuario criado = Usuario.builder().id(novoId).email("novo@awosbank.com").build();
        when(pjp.getArgs()).thenReturn(new Object[]{cadastro});
        when(pjp.proceed()).thenReturn(criado);

        aspect.auditar(pjp, auditado);

        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq(novoId), eq("novo@awosbank.com"),
                eq(true), any());
    }

    @Test
    void semUsuarioNemArgumentoIdentificavelOAtorEhAnonimo() {
        assertEquals("anonimo", AuditoriaAspect.ator(new Object[]{"qualquer coisa"}));
    }

    @Test
    void detalhesVaziosQuandoNaoHaNadaParaDescrever() {
        assertEquals("", AuditoriaAspect.detalhes(new Object[]{"x"}, null, null));
    }
}
