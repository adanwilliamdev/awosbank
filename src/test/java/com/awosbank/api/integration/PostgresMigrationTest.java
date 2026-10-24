package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.dto.ExtratoFiltroDTO;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração contra um PostgreSQL de verdade (Testcontainers).
 *
 * <p>Sobe a aplicação com Flyway ligado e {@code ddl-auto=validate}: se as migrations divergirem das
 * entidades JPA, o contexto nem inicia. Também exercita o que o H2 não prova: locks reais
 * ({@code SELECT ... FOR UPDATE}), constraints e o trigger de imutabilidade da auditoria.
 *
 * <p>É ignorado automaticamente quando não há Docker disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class PostgresMigrationTest {

    private static final String SENHA = "senha123";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired ContaRepository contaRepository;

    private TransferenciaDTO dto(Usuario destino, String valor) {
        TransferenciaDTO d = new TransferenciaDTO();
        d.setNumeroContaDestino(destino.getConta().getNumeroConta());
        d.setValor(new BigDecimal(valor));
        d.setTipo(TipoTransacao.PIX);
        d.setSenha(SENHA);
        return d;
    }

    @Test
    void todasAsMigrationsForamAplicadasComSucesso() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertNotNull(aplicadas);
        assertTrue(aplicadas >= 2, "esperava V1 (schema) e V2 (auditoria), veio " + aplicadas);
    }

    @Test
    void fluxoCompletoFuncionaNoPostgres() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("300.00"));
        transacaoService.transferir(a, dto(b, "120.00"));

        assertEquals(0, contaRepository.findById(a.getConta().getId()).orElseThrow().getSaldo()
                .compareTo(new BigDecimal("180.00")));
        // consulta com parâmetro de tipo nulo (a que mais costuma quebrar entre bancos)
        assertEquals(2, transacaoService.buscarExtrato(a.getConta().getId(), new ExtratoFiltroDTO()).size());

        Integer registrosDeAuditoria = jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE usuario_email = ?", Integer.class, a.getEmail());
        assertEquals(3, registrosDeAuditoria, "cadastro + depósito + transferência");
    }

    @Test
    void auditoriaEhAppendOnly() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);

        DataAccessException update = assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE auditoria SET detalhes = 'adulterado' WHERE usuario_email = ?", a.getEmail()));
        assertTrue(update.getMessage().contains("append-only"));

        DataAccessException delete = assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM auditoria WHERE usuario_email = ?", a.getEmail()));
        assertTrue(delete.getMessage().contains("append-only"));
    }

    @Test
    void bancoRecusaSaldoNegativoMesmoSeAAplicacaoFalhar() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);

        assertThrows(DataAccessException.class,
                () -> jdbc.update("UPDATE contas SET saldo = -1 WHERE id = ?", a.getConta().getId()));
    }

    @Test
    void lockPessimistaImpedeGastoDuploNoPostgres() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("100.00"));

        int tentativas = 8;
        ExecutorService pool = Executors.newFixedThreadPool(tentativas);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < tentativas; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                try {
                    transacaoService.transferir(a, dto(b, "100.00"));
                    sucessos.incrementAndGet();
                } catch (BusinessException esperado) {
                    assertEquals("Saldo insuficiente", esperado.getMessage());
                }
                return null;
            }));
        }
        largada.countDown();
        try {
            for (Future<?> f : futuros) f.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, sucessos.get(), "só uma transferência de R$ 100 cabe no saldo");
        assertEquals(0, contaRepository.findById(a.getConta().getId()).orElseThrow().getSaldo()
                .compareTo(BigDecimal.ZERO));
        assertEquals(0, contaRepository.findById(b.getConta().getId()).orElseThrow().getSaldo()
                .compareTo(new BigDecimal("100.00")));
    }

    @Test
    void transferenciasCruzadasConservamCadaSaldoNoPostgres() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("1000.00"));
        transacaoService.depositar(b, new BigDecimal("1000.00"));

        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final boolean aParaB = i % 2 == 0;
            futuros.add(pool.submit(() -> {
                largada.await();
                for (int k = 0; k < 10; k++) {
                    if (aParaB) transacaoService.transferir(a, dto(b, "10.00"));
                    else transacaoService.transferir(b, dto(a, "10.00"));
                }
                return null;
            }));
        }
        largada.countDown();
        try {
            for (Future<?> f : futuros) f.get(90, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertEquals(0, contaRepository.findById(a.getConta().getId()).orElseThrow().getSaldo()
                .compareTo(new BigDecimal("1000.00")), "10 idas e 10 voltas se anulam em cada conta");
        assertEquals(0, contaRepository.findById(b.getConta().getId()).orElseThrow().getSaldo()
                .compareTo(new BigDecimal("1000.00")));
    }
}
