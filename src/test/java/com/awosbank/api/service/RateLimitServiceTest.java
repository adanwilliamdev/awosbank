package com.awosbank.api.service;

import com.awosbank.api.service.RateLimitService.Decisao;
import com.awosbank.api.service.RateLimitService.Grupo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    /** Relógio que só anda quando o teste manda. */
    private static final class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private final RelogioManual relogio = new RelogioManual();

    private RateLimitService servico(boolean habilitado, int maxAuth, int maxApi) {
        return new RateLimitService(habilitado, maxAuth, maxApi, Duration.ofSeconds(60), relogio);
    }

    @Test
    void permiteAteOLimiteEBloqueiaDepois() {
        RateLimitService s = servico(true, 3, 10);

        for (int i = 0; i < 3; i++) assertTrue(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());

        Decisao bloqueada = s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");
        assertFalse(bloqueada.permitido());
        assertEquals(60, bloqueada.retryAfterSegundos());
    }

    @Test
    void retryAfterDiminuiComOPassarDoTempo() {
        RateLimitService s = servico(true, 1, 10);
        s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");

        relogio.avancar(Duration.ofSeconds(45));

        Decisao d = s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");
        assertFalse(d.permitido());
        assertEquals(15, d.retryAfterSegundos());
    }

    @Test
    void liberaQuandoAJanelaExpira() {
        RateLimitService s = servico(true, 2, 10);
        s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");
        s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");
        assertFalse(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());

        relogio.avancar(Duration.ofSeconds(60));

        assertTrue(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());
    }

    @Test
    void requisicoesRecusadasNaoEstendemOBloqueio() {
        RateLimitService s = servico(true, 1, 10);
        s.verificar(Grupo.AUTENTICACAO, "1.1.1.1");

        // insiste durante a janela inteira: nada disso pode empurrar o fim do bloqueio
        for (int i = 0; i < 5; i++) {
            relogio.avancar(Duration.ofSeconds(10));
            assertFalse(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());
        }
        relogio.avancar(Duration.ofSeconds(10)); // 60 s desde a única requisição aceita

        assertTrue(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());
    }

    @Test
    void ipsEGruposTemContagensIndependentes() {
        RateLimitService s = servico(true, 1, 1);
        assertTrue(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());

        assertFalse(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());
        assertTrue(s.verificar(Grupo.AUTENTICACAO, "2.2.2.2").permitido(), "outro IP");
        assertTrue(s.verificar(Grupo.API, "1.1.1.1").permitido(), "outro grupo, mesmo IP");
    }

    @Test
    void desabilitadoNuncaBloqueia() {
        RateLimitService s = servico(false, 1, 1);
        for (int i = 0; i < 50; i++) assertTrue(s.verificar(Grupo.AUTENTICACAO, "1.1.1.1").permitido());
    }

    @Test
    void ipNuloOuVazioEhAgrupadoComoDesconhecido() {
        RateLimitService s = servico(true, 1, 10);
        assertTrue(s.verificar(Grupo.AUTENTICACAO, null).permitido());
        assertFalse(s.verificar(Grupo.AUTENTICACAO, " ").permitido());
    }
}
