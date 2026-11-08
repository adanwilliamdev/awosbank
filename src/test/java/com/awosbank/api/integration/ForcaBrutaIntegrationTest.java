package com.awosbank.api.integration;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.service.ProtecaoForcaBrutaService;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import com.awosbank.api.domain.repository.ContaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bloqueio temporário contra força bruta de ponta a ponta (limites padrão: 5 falhas em 15 min => 15 min
 * de bloqueio). O fim do bloqueio é simulado voltando a data gravada no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForcaBrutaIntegrationTest {

    private static final String SENHA = "senha123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired ProtecaoForcaBrutaService protecao;
    @Autowired ContaRepository contaRepository;
    @Autowired JdbcTemplate jdbc;

    private void login(String email, String senha, String urlEsperada) throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", email).param("password", senha))
                .andExpect(redirectedUrl(urlEsperada));
    }

    private void expirarBloqueio(String coluna, String email) {
        jdbc.update("UPDATE usuarios SET " + coluna + " = ? WHERE email = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), email);
    }

    private String corpoTransferencia(Usuario destino, String senha) {
        return "{\"numeroContaDestino\":\"" + destino.getConta().getNumeroConta()
                + "\",\"valor\":10.00,\"tipo\":\"PIX\",\"senha\":\"" + senha + "\"}";
    }

    private void transferir(Usuario origem, Usuario destino, String senha) throws Exception {
        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, senha)));
    }

    private BigDecimal saldo(Usuario u) {
        return contaRepository.findById(u.getConta().getId()).orElseThrow().getSaldo();
    }

    // ------------------------------------------------------------------ login

    @Test
    void cincoSenhasErradasNoLoginBloqueiamEmNemASenhaCertaEntra() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 5; i++) login(u.getEmail(), "errada", "/login?error");
        login(u.getEmail(), SENHA, "/login?error");   // senha CERTA, mas bloqueado
    }

    @Test
    void depoisDoBloqueioExpirarOLoginVoltaAFuncionarEOContadorZera() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);
        for (int i = 0; i < 5; i++) login(u.getEmail(), "errada", "/login?error");
        login(u.getEmail(), SENHA, "/login?error");

        expirarBloqueio("login_bloqueado_ate", u.getEmail());

        login(u.getEmail(), SENHA, "/dashboard");
        Integer falhas = jdbc.queryForObject("SELECT falhas_login FROM usuarios WHERE email = ?", Integer.class, u.getEmail());
        assertEquals(0, falhas, "login correto zera o contador");
    }

    @Test
    void loginCorretoNoMeioZeraAContagemAnterior() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 4; i++) login(u.getEmail(), "errada", "/login?error");
        login(u.getEmail(), SENHA, "/dashboard");            // zera
        for (int i = 0; i < 4; i++) login(u.getEmail(), "errada", "/login?error");
        login(u.getEmail(), SENHA, "/dashboard");            // 4 + 4 falhas NÃO bloqueiam: o meio zerou
    }

    @Test
    void bloqueioDeUmUsuarioNaoAfetaOutro() throws Exception {
        Usuario alvo = TestData.novoUsuario(usuarioService, SENHA);
        Usuario outro = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 6; i++) login(alvo.getEmail(), "errada", "/login?error");

        login(outro.getEmail(), SENHA, "/dashboard");
    }

    @Test
    void emailInexistenteRecebeAMesmaRespostaSemRevelarSeAContaExiste() throws Exception {
        // mesmo redirecionamento de sempre, a qualquer número de tentativas
        for (int i = 0; i < 8; i++) login("naoexiste-" + System.nanoTime() + "@awosbank.test", "x", "/login?error");
        String fantasma = "fantasma@awosbank.test";
        for (int i = 0; i < 8; i++) login(fantasma, "x", "/login?error");
    }

    @Test
    void falhasDeLoginNaoTravamAsOperacoesDeQuemJaEstaLogado() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("100.00"));

        for (int i = 0; i < 6; i++) login(origem.getEmail(), "errada", "/login?error");   // login bloqueado

        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, SENHA)))
                .andExpect(status().isOk());      // contadores independentes
    }

    // ------------------------------------------------------ /api/transferencia

    @Test
    void sessaoRoubadaNaoConsegueTestarSenhasIndefinidamenteNaTransferencia() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("100.00"));

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, "errada")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value("Senha incorreta"));
        }

        // 6ª tentativa, agora com a senha CERTA: recusada pelo bloqueio, e nada foi movimentado
        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, SENHA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Muitas tentativas")));
        assertEquals(0, saldo(origem).compareTo(new BigDecimal("100.00")));
        assertEquals(0, saldo(destino).compareTo(BigDecimal.ZERO));
    }

    @Test
    void transferenciaVoltaAoNormalQuandoOBloqueioExpira() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("100.00"));
        for (int i = 0; i < 5; i++) transferir(origem, destino, "errada");

        expirarBloqueio("senha_op_bloqueada_ate", origem.getEmail());

        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, SENHA)))
                .andExpect(status().isOk());
        assertEquals(0, saldo(origem).compareTo(new BigDecimal("90.00")));
    }

    @Test
    void senhaCertaNaTransferenciaZeraAsFalhasAnteriores() throws Exception {
        Usuario origem = TestData.novoUsuario(usuarioService, SENHA);
        Usuario destino = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(origem, new BigDecimal("100.00"));

        for (int i = 0; i < 4; i++) transferir(origem, destino, "errada");
        transferir(origem, destino, SENHA);                      // zera
        for (int i = 0; i < 4; i++) transferir(origem, destino, "errada");

        mvc.perform(post("/api/transferencia").with(user(origem.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpoTransferencia(destino, SENHA)))
                .andExpect(status().isOk());                     // 4 + 4 não bloqueiam
    }

    @Test
    void oMesmoLimiteProtegeOEncerramentoDeConta() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/conta/encerrar").with(user(u.getEmail())).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"errada\"}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/conta/encerrar").with(user(u.getEmail())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Muitas tentativas")));
    }

    // ------------------------------------------------------------ concorrência

    @Test
    void tentativasEmParaleloSaoTodasContadasEBloqueiam() throws Exception {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futuros.add(pool.submit(() -> {
                largada.await();
                protecao.registrarFalhaSenhaOp(u.getEmail());
                return null;
            }));
        }
        largada.countDown();
        try {
            for (Future<?> f : futuros) f.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        Integer falhas = jdbc.queryForObject("SELECT falhas_senha_op FROM usuarios WHERE email = ?", Integer.class, u.getEmail());
        assertTrue(falhas >= 5, "nenhuma falha pode se perder na corrida (contou " + falhas + ")");
        assertNotNull(jdbc.queryForObject("SELECT senha_op_bloqueada_ate FROM usuarios WHERE email = ?", Timestamp.class, u.getEmail()));
        assertThrows(com.awosbank.api.exception.BusinessException.class, () -> protecao.exigirOperacaoLiberada(u.getEmail()));
    }

    @Test
    void salvarOutroCampoDoUsuarioNaoSobrescreveOBloqueioGravadoPorUpdateDireto() {
        Usuario u = TestData.novoUsuario(usuarioService, SENHA);

        // Mesma transação/contexto de persistência: a entidade é carregada com contador = 0, os UPDATEs diretos
        // mudam o banco por baixo dela e só então um campo comum é alterado e gravado (@DynamicUpdate).
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Usuario carregado = usuarioRepository.findByEmail(u.getEmail()).orElseThrow();
            for (int i = 0; i < 5; i++) protecao.registrarFalhaSenhaOp(u.getEmail());
            carregado.setTelefone("11999990000");
            usuarioRepository.saveAndFlush(carregado);
        });

        assertThrows(com.awosbank.api.exception.BusinessException.class, () -> protecao.exigirOperacaoLiberada(u.getEmail()));
    }

    @Autowired com.awosbank.api.domain.repository.UsuarioRepository usuarioRepository;
    @Autowired PlatformTransactionManager transactionManager;
}
