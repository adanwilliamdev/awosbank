package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.dto.ExtratoFiltroDTO;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

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
        assertTrue(aplicadas >= 4, "esperava V1 (schema), V2 (auditoria), V3 (usuario_id) e V4 (idempotência), veio " + aplicadas);
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
    void chaveDeIdempotenciaEhUnicaPorContaDeOrigemNoBanco() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("500.00"));
        String chave = "pg-" + java.util.UUID.randomUUID();

        TransferenciaDTO d = dto(b, "50.00");
        d.setChaveIdempotencia(chave);
        var primeira = transacaoService.transferir(a, d);
        var repetida = transacaoService.transferir(a, d);

        assertEquals(primeira.getComprovanteCodigo(), repetida.getComprovanteCodigo());
        Integer linhas = jdbc.queryForObject(
                "SELECT count(*) FROM transacoes WHERE chave_idempotencia = ?", Integer.class, chave);
        assertEquals(1, linhas, "só uma linha gravada para a chave");
        // depósitos/saques (sem chave, NULL) não colidem entre si na UNIQUE
        transacaoService.depositar(a, new BigDecimal("1.00"));
        transacaoService.depositar(a, new BigDecimal("1.00"));

        // a constraint barra duplicata mesmo que o código esqueça de consultar a chave
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.update(
                "INSERT INTO transacoes (id, conta_origem_id, conta_destino_id, valor, tipo, data_hora,"
                        + " comprovante_codigo, chave_idempotencia) VALUES (?, ?, ?, 1.00, 'PIX', now(), ?, ?)",
                java.util.UUID.randomUUID(), a.getConta().getId(), b.getConta().getId(),
                "CMP-DUP-" + java.util.UUID.randomUUID().toString().substring(0, 8), chave));
    }

    @Test
    void auditoriaGuardaOIdDoUsuarioSemForeignKey() {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);

        Integer comId = jdbc.queryForObject(
                "SELECT count(*) FROM auditoria WHERE usuario_id = ?", Integer.class, a.getId());
        assertEquals(1, comId, "o CADASTRO fica vinculado ao ID do usuário");
    }

    /**
     * Aplica V1+V2, insere dados "legados" (sem usuario_id) e só então aplica a V3, para provar o backfill:
     * registros posteriores à abertura da conta passam a ser do usuário atual do e-mail; os anteriores
     * (titular antigo / tentativas pré-cadastro) e os de e-mails sem usuário ficam sem dono.
     */
    @Test
    void migrationV3FazBackfillSoDoQueEPosteriorAAberturaDaConta() {
        String schema = "backfill_v3";
        Flyway ate2 = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).target("2").load();
        ate2.migrate();

        JdbcTemplate legado = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl() + "&currentSchema=" + schema, POSTGRES.getUsername(), POSTGRES.getPassword()));

        UUID contaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime abertura = LocalDateTime.of(2026, 1, 10, 10, 0);
        legado.update("INSERT INTO contas (id, numero_conta, agencia, saldo, tipo_conta, status, data_abertura) "
                        + "VALUES (?, ?, '0001', 0, 'CORRENTE', 'ATIVA', ?)",
                contaId, "900001", Timestamp.valueOf(abertura));
        legado.update("INSERT INTO usuarios (id, nome_completo, cpf_cnpj, email, senha, conta_id) VALUES (?, ?, ?, ?, ?, ?)",
                usuarioId, "Reuso", "12345678909", "reuso@x.test", "hash", contaId);

        String insercao = "INSERT INTO auditoria (id, data_hora, usuario_email, acao, sucesso) VALUES (?, ?, ?, 'LOGIN_SUCESSO', true)";
        legado.update(insercao, UUID.randomUUID(), Timestamp.valueOf(abertura.minusDays(5)), "reuso@x.test"); // titular antigo
        legado.update(insercao, UUID.randomUUID(), Timestamp.valueOf(abertura.plusDays(1)), "reuso@x.test");  // titular atual
        legado.update(insercao, UUID.randomUUID(), Timestamp.valueOf(abertura.plusDays(1)), "ninguem@x.test"); // sem usuário

        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).load().migrate();

        assertEquals(1, legado.queryForObject(
                "SELECT count(*) FROM auditoria WHERE usuario_id = ?", Integer.class, usuarioId),
                "só o registro posterior à abertura da conta é atribuído ao usuário atual");
        assertEquals(2, legado.queryForObject(
                "SELECT count(*) FROM auditoria WHERE usuario_id IS NULL", Integer.class),
                "o registro do titular antigo e o de e-mail sem usuário continuam sem dono");

        // a tabela voltou a ser append-only depois do backfill
        assertThrows(DataAccessException.class, () -> legado.update("UPDATE auditoria SET detalhes = 'x'"));
    }

    /**
     * No PostgreSQL de verdade: a violação da UNIQUE só aparece no INSERT (saveAndFlush) e a transação fica
     * abortada; quem perde a corrida recebe a mesma mensagem de negócio da checagem prévia — e o formato do
     * erro do banco (constraint/coluna) é o que a classificação por e-mail x CPF espera.
     */
    @Test
    void cadastrosSimultaneosComOMesmoEmailRecebemMensagemDeNegocioNoPostgres() throws Exception {
        String email = "corrida-" + System.nanoTime() + "@awosbank.test";
        List<CadastroDTO> dtos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int semente = TestData.proximaSemente();
            dtos.add(TestData.cadastroDto(semente, email, TestData.cpfValido(semente), SENHA));
        }

        List<Object> resultados = TestData.cadastrarEmParalelo(usuarioService, dtos);

        assertEquals(1, resultados.stream().filter(o -> o instanceof Usuario).count(), resultados.toString());
        for (Object r : resultados) {
            if (r instanceof Throwable t) {
                assertInstanceOf(BusinessException.class, t, resultados.toString());
                assertEquals("Email já cadastrado", t.getMessage());
            }
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE email = ?", Integer.class, email));
    }

    @Test
    void cadastrosSimultaneosComOMesmoDocumentoRecebemMensagemDeNegocioNoPostgres() throws Exception {
        String cpf = TestData.cpfValido(TestData.proximaSemente());
        List<CadastroDTO> dtos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            int semente = TestData.proximaSemente();
            dtos.add(TestData.cadastroDto(semente, "cpf-corrida-" + semente + "-" + System.nanoTime() + "@awosbank.test", cpf, SENHA));
        }

        List<Object> resultados = TestData.cadastrarEmParalelo(usuarioService, dtos);

        assertEquals(1, resultados.stream().filter(o -> o instanceof Usuario).count(), resultados.toString());
        for (Object r : resultados) {
            if (r instanceof Throwable t) {
                assertInstanceOf(BusinessException.class, t, resultados.toString());
                assertEquals("CPF/CNPJ já cadastrado", t.getMessage());
            }
        }
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM usuarios WHERE cpf_cnpj = ?", Integer.class, cpf));
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
