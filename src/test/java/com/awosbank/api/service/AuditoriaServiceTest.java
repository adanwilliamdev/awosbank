package com.awosbank.api.service;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.repository.AuditoriaRepository;
import com.awosbank.api.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditoriaServiceTest {

    private AuditoriaRepository repository;
    private UsuarioRepository usuarioRepository;
    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AuditoriaRepository.class);
        usuarioRepository = Mockito.mock(UsuarioRepository.class);
        service = new AuditoriaService(repository, usuarioRepository, Mockito.mock(PlatformTransactionManager.class));
    }

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private Auditoria salvo() {
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void gravaRegistroComEmailNormalizadoEDataPreenchida() {
        service.registrar(AcaoAuditoria.DEPOSITO, null, "  Cliente@AwosBank.com ", true, "valor=10.00");

        Auditoria a = salvo();
        assertEquals("cliente@awosbank.com", a.getUsuarioEmail());
        assertEquals(AcaoAuditoria.DEPOSITO, a.getAcao());
        assertTrue(a.isSucesso());
        assertEquals("valor=10.00", a.getDetalhes());
        assertNotNull(a.getDataHora());
        assertNull(a.getIpOrigem(), "fora de uma requisição HTTP não há IP");
    }

    @Test
    void usaAnonimoQuandoNaoHaUsuario() {
        service.registrar(AcaoAuditoria.LOGIN_FALHA, null, null, false, null);
        assertEquals("anonimo", salvo().getUsuarioEmail());
    }

    @Test
    void truncaDetalhesLongos() {
        service.registrar(AcaoAuditoria.SAQUE, null, "a@b.com", false, "x".repeat(900));
        assertEquals(500, salvo().getDetalhes().length());
    }

    @Test
    void registraOIpDaRequisicaoAtual() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(AcaoAuditoria.LOGIN_SUCESSO, null, "a@b.com", true, null);

        assertEquals("203.0.113.10", salvo().getIpOrigem());
    }

    @Test
    void falhaAoGravarNaoPropagaExcecao() {
        when(repository.save(any(Auditoria.class))).thenThrow(new IllegalStateException("banco fora do ar"));

        assertDoesNotThrow(() -> service.registrar(AcaoAuditoria.TRANSFERENCIA, null, "a@b.com", true, null));
    }

    @Test
    void gravaOIdDoUsuarioNoRegistro() {
        UUID id = UUID.randomUUID();

        service.registrar(AcaoAuditoria.DEPOSITO, id, "a@b.com", true, null);

        assertEquals(id, salvo().getUsuarioId());
    }

    @Test
    void registrarPorEmailResolveOIdDoUsuario() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findIdByEmail("a@b.com")).thenReturn(Optional.of(id));

        service.registrarPorEmail(AcaoAuditoria.LOGIN_SUCESSO, " A@B.com ", true, null);

        Auditoria a = salvo();
        assertEquals(id, a.getUsuarioId());
        assertEquals("a@b.com", a.getUsuarioEmail());
    }

    @Test
    void registrarPorEmailSemUsuarioCadastradoGravaSemDono() {
        when(usuarioRepository.findIdByEmail("fantasma@x.com")).thenReturn(Optional.empty());

        service.registrarPorEmail(AcaoAuditoria.LOGIN_FALHA, "fantasma@x.com", false, null);

        assertNull(salvo().getUsuarioId());
    }

    @Test
    void buscarIdPorEmailNormalizaEhNaoConsultaComEmailVazio() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findIdByEmail("a@b.com")).thenReturn(Optional.of(id));

        assertEquals(id, service.buscarIdPorEmail("  A@B.COM "));
        assertNull(service.buscarIdPorEmail(null));
        assertNull(service.buscarIdPorEmail("  "));
        verify(usuarioRepository, times(1)).findIdByEmail(any());
    }

    @Test
    void falhaAoResolverOIdNaoPropagaExcecao() {
        when(usuarioRepository.findIdByEmail(any())).thenThrow(new IllegalStateException("banco fora do ar"));

        assertNull(assertDoesNotThrow(() -> service.buscarIdPorEmail("a@b.com")));
    }

    @Test
    void listarDelegaAoRepositorioPeloIdDoUsuario() {
        UUID id = UUID.randomUUID();
        PageRequest pagina = PageRequest.of(0, 10);
        Page<Auditoria> esperado = new PageImpl<>(List.of());
        when(repository.findByUsuarioIdOrderByDataHoraDesc(id, pagina)).thenReturn(esperado);

        assertSame(esperado, service.listarDoUsuario(id, pagina));
    }

    @Test
    void listarSemIdDevolvePaginaVaziaSemConsultarORepositorio() {
        // Com ID nulo o Spring Data geraria "IS NULL" e vazaria registros sem dono.
        Page<Auditoria> resultado = service.listarDoUsuario(null, PageRequest.of(0, 10));

        assertTrue(resultado.isEmpty());
        verifyNoInteractions(repository);
    }
}
