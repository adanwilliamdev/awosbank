package com.awosbank.api.repository;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.enums.TipoTransacao;
import com.awosbank.api.domain.model.Auditoria;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.repository.AuditoriaRepository;
import com.awosbank.api.domain.repository.ContaRepository;
import com.awosbank.api.domain.repository.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Consultas dos repositórios contra um banco real (H2 em memória, só JPA — sem contexto web). */
@DataJpaTest
@ActiveProfiles("test")
class RepositoriosJpaTest {

    @Autowired ContaRepository contaRepository;
    @Autowired TransacaoRepository transacaoRepository;
    @Autowired AuditoriaRepository auditoriaRepository;

    Conta a;
    Conta b;
    LocalDateTime agora;

    @BeforeEach
    void setUp() {
        agora = LocalDateTime.now();
        a = contaRepository.save(conta("111111-1"));
        b = contaRepository.save(conta("222222-2"));
    }

    private Conta conta(String numero) {
        return Conta.builder().numeroConta(numero).agencia("0001")
                .tipoConta(TipoConta.CORRENTE).saldo(new BigDecimal("100.00")).build();
    }

    private Transacao tx(Conta origem, Conta destino, String valor, TipoTransacao tipo, LocalDateTime quando, String codigo) {
        return transacaoRepository.save(Transacao.builder()
                .contaOrigem(origem).contaDestino(destino).valor(new BigDecimal(valor))
                .tipo(tipo).dataHora(quando).comprovanteCodigo(codigo).build());
    }

    @Test
    void contaPorNumeroEExistencia() {
        assertTrue(contaRepository.existsByNumeroConta("111111-1"));
        assertFalse(contaRepository.existsByNumeroConta("999999-9"));
        assertEquals(a.getId(), contaRepository.findByNumeroConta("111111-1").orElseThrow().getId());
    }

    @Test
    void carregaContaComLockParaAtualizacao() {
        assertEquals(b.getId(), contaRepository.findByIdForUpdate(b.getId()).orElseThrow().getId());
    }

    @Test
    void extratoTrazOrigemEDestinoDoMaisRecentePraMaisAntigo() {
        tx(a, b, "10.00", TipoTransacao.PIX, agora.minusDays(3), "CMP-1");
        tx(b, a, "20.00", TipoTransacao.TED, agora.minusDays(1), "CMP-2");
        tx(null, a, "30.00", TipoTransacao.DEPOSITO, agora.minusDays(2), "CMP-3");

        List<Transacao> extrato = transacaoRepository.buscarExtrato(a.getId(), agora.minusDays(10), agora, null);

        assertEquals(List.of("CMP-2", "CMP-3", "CMP-1"),
                extrato.stream().map(Transacao::getComprovanteCodigo).toList());
    }

    @Test
    void extratoFiltraPorTipoEPeriodo() {
        tx(a, b, "10.00", TipoTransacao.PIX, agora.minusDays(20), "CMP-ANTIGA");
        tx(a, b, "10.00", TipoTransacao.PIX, agora.minusDays(1), "CMP-PIX");
        tx(a, b, "10.00", TipoTransacao.TED, agora.minusDays(1), "CMP-TED");

        List<Transacao> soPix = transacaoRepository.buscarExtrato(
                a.getId(), agora.minusDays(5), agora, TipoTransacao.PIX);

        assertEquals(List.of("CMP-PIX"), soPix.stream().map(Transacao::getComprovanteCodigo).toList());
    }

    @Test
    void extratoNaoMostraTransacoesDeOutrasContas() {
        Conta c = contaRepository.save(conta("333333-3"));
        tx(b, c, "10.00", TipoTransacao.PIX, agora.minusDays(1), "CMP-B-C");

        assertTrue(transacaoRepository.buscarExtrato(a.getId(), agora.minusDays(5), agora, null).isEmpty());
    }

    @Test
    void ultimasTransacoesRespeitamOLimite() {
        for (int i = 0; i < 5; i++) {
            tx(a, b, "1.00", TipoTransacao.PIX, agora.minusHours(i), "CMP-" + i);
        }
        List<Transacao> ultimas = transacaoRepository.buscarUltimas(a.getId(), PageRequest.of(0, 3));

        assertEquals(3, ultimas.size());
        assertEquals("CMP-0", ultimas.get(0).getComprovanteCodigo());
    }

    @Test
    void comprovantePorCodigo() {
        tx(a, b, "10.00", TipoTransacao.PIX, agora, "CMP-ABC");
        assertTrue(transacaoRepository.findByComprovanteCodigo("CMP-ABC").isPresent());
        assertTrue(transacaoRepository.findByComprovanteCodigo("NAO-EXISTE").isEmpty());
    }

    @Test
    void auditoriaListaSoDoUsuarioEmOrdemDecrescenteComPaginacao() {
        grava("ana@x.com", AcaoAuditoria.LOGIN_SUCESSO, agora.minusMinutes(3));
        grava("ana@x.com", AcaoAuditoria.DEPOSITO, agora.minusMinutes(2));
        grava("ana@x.com", AcaoAuditoria.SAQUE, agora.minusMinutes(1));
        grava("bia@x.com", AcaoAuditoria.TRANSFERENCIA, agora);

        Page<Auditoria> pagina = auditoriaRepository.findByUsuarioEmailOrderByDataHoraDesc("ana@x.com", PageRequest.of(0, 2));

        assertEquals(3, pagina.getTotalElements());
        assertEquals(List.of(AcaoAuditoria.SAQUE, AcaoAuditoria.DEPOSITO),
                pagina.getContent().stream().map(Auditoria::getAcao).toList());
        assertEquals(1, auditoriaRepository.countByUsuarioEmailAndAcao("bia@x.com", AcaoAuditoria.TRANSFERENCIA));
    }

    private void grava(String email, AcaoAuditoria acao, LocalDateTime quando) {
        auditoriaRepository.save(Auditoria.builder().usuarioEmail(email).acao(acao).sucesso(true).dataHora(quando).build());
    }
}
