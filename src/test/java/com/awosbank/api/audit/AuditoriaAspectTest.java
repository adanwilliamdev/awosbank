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
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
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
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq("cliente@awosbank.com"),
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
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq("cliente@awosbank.com"),
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
        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq("anonimo"), eq(false), detalhes.capture());
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

        verify(auditoriaService).registrar(eq(AcaoAuditoria.TRANSFERENCIA), eq("logado@awosbank.com"), eq(true),
                eq("valor=5.00"));
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
