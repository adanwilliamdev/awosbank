package com.awosbank.api.service;

import com.awosbank.api.TestData;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prova a correção do lock pessimista: várias threads disputando o mesmo saldo,
 * contra o H2 de verdade (sem mocks). Sem o lock, estes testes falham.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransferenciaConcorrenciaTest {

    private static final String SENHA = "senha123";

    @Autowired UsuarioService usuarioService;
    @Autowired TransacaoService transacaoService;
    @Autowired ContaRepository contaRepository;

    private BigDecimal saldo(Usuario u) {
        return contaRepository.findById(u.getConta().getId()).orElseThrow().getSaldo();
    }

    private TransferenciaDTO dto(Usuario destino, String valor) {
        TransferenciaDTO d = new TransferenciaDTO();
        d.setNumeroContaDestino(destino.getConta().getNumeroConta());
        d.setValor(new BigDecimal(valor));
        d.setTipo(TipoTransacao.PIX);
        d.setSenha(SENHA);
        return d;
    }

    /** Executa as tarefas todas ao mesmo tempo (largada sincronizada) e falha se travar (deadlock). */
    private void executarEmParalelo(List<Callable<Void>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Void>> futuros = new ArrayList<>();
        for (Callable<Void> t : tarefas) {
            futuros.add(pool.submit(() -> { largada.await(); return t.call(); }));
        }
        largada.countDown();
        try {
            for (Future<Void> f : futuros) f.get(60, TimeUnit.SECONDS);   // TimeoutException = deadlock
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void dezTransferenciasSimultaneasComSaldoParaUmaSoUmaPassa() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("100.00"));

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger saldoInsuficiente = new AtomicInteger();
        List<Callable<Void>> tarefas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tarefas.add(() -> {
                try {
                    transacaoService.transferirPorIdentificador(a, dto(b, "100.00"));
                    sucessos.incrementAndGet();
                } catch (BusinessException e) {
                    if ("Saldo insuficiente".equals(e.getMessage())) saldoInsuficiente.incrementAndGet();
                    else throw e;
                }
                return null;
            });
        }
        executarEmParalelo(tarefas);

        assertEquals(1, sucessos.get(), "só uma transferência de R$ 100 cabe no saldo");
        assertEquals(9, saldoInsuficiente.get());
        assertEquals(0, saldo(a).compareTo(BigDecimal.ZERO), "origem não pode ficar negativa");
        assertEquals(0, saldo(b).compareTo(new BigDecimal("100.00")), "destino recebe exatamente uma vez");
    }

    @Test
    void depositosSimultaneosNaoPerdemAtualizacao() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);

        List<Callable<Void>> tarefas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tarefas.add(() -> { transacaoService.depositar(a, new BigDecimal("10.00")); return null; });
        }
        executarEmParalelo(tarefas);

        assertEquals(0, saldo(a).compareTo(new BigDecimal("200.00")), "20 x R$ 10 = R$ 200 (sem lost update)");
    }

    @Test
    void transferenciasCruzadasNaoCausamDeadlockEConservamOTotal() throws Exception {
        Usuario a = TestData.novoUsuario(usuarioService, SENHA);
        Usuario b = TestData.novoUsuario(usuarioService, SENHA);
        transacaoService.depositar(a, new BigDecimal("1000.00"));
        transacaoService.depositar(b, new BigDecimal("1000.00"));

        List<Callable<Void>> tarefas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final boolean aParaB = i % 2 == 0;
            tarefas.add(() -> {
                for (int k = 0; k < 10; k++) {
                    if (aParaB) transacaoService.transferirPorIdentificador(a, dto(b, "10.00"));
                    else transacaoService.transferirPorIdentificador(b, dto(a, "10.00"));
                }
                return null;
            });
        }
        executarEmParalelo(tarefas);   // lança se alguma thread falhar ou travar

        BigDecimal total = saldo(a).add(saldo(b));
        assertEquals(0, total.compareTo(new BigDecimal("2000.00")), "dinheiro não pode ser criado nem destruído");
        assertEquals(0, saldo(a).compareTo(new BigDecimal("1000.00")), "10 idas e 10 voltas se anulam");
    }
}
