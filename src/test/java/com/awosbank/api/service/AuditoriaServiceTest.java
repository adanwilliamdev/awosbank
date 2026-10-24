package com.awosbank.api.service;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.repository.AuditoriaRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditoriaServiceTest {

    private AuditoriaRepository repository;
    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AuditoriaRepository.class);
        service = new AuditoriaService(repository, Mockito.mock(PlatformTransactionManager.class));
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
        service.registrar(AcaoAuditoria.DEPOSITO, "  Cliente@AwosBank.com ", true, "valor=10.00");

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
        service.registrar(AcaoAuditoria.LOGIN_FALHA, null, false, null);
        assertEquals("anonimo", salvo().getUsuarioEmail());
    }

    @Test
    void truncaDetalhesLongos() {
        service.registrar(AcaoAuditoria.SAQUE, "a@b.com", false, "x".repeat(900));
        assertEquals(500, salvo().getDetalhes().length());
    }

    @Test
    void registraOIpDaRequisicaoAtual() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.registrar(AcaoAuditoria.LOGIN_SUCESSO, "a@b.com", true, null);

        assertEquals("203.0.113.10", salvo().getIpOrigem());
    }

    @Test
    void falhaAoGravarNaoPropagaExcecao() {
        when(repository.save(any(Auditoria.class))).thenThrow(new IllegalStateException("banco fora do ar"));

        assertDoesNotThrow(() -> service.registrar(AcaoAuditoria.TRANSFERENCIA, "a@b.com", true, null));
    }

    @Test
    void listarDelegaAoRepositorioComEmailNormalizado() {
        PageRequest pagina = PageRequest.of(0, 10);
        Page<Auditoria> esperado = new PageImpl<>(List.of());
        when(repository.findByUsuarioEmailOrderByDataHoraDesc("a@b.com", pagina)).thenReturn(esperado);

        assertSame(esperado, service.listarDoUsuario("A@B.com", pagina));
    }
}
