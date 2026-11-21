package com.awosbank.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP, em memória, com janela deslizante.
 *
 * <p>Complementa o bloqueio por conta ({@link ProtecaoForcaBrutaService}): aquele impede insistir em UMA
 * conta; este impede um mesmo IP de martelar o login/cadastro (ex.: testar uma senha em várias contas) ou
 * a API. Requisições recusadas <b>não</b> entram na contagem, então o bloqueio não se estende sozinho.
 *
 * <p>Limitação consciente: o estado vive na memória da instância. Com várias instâncias cada uma conta
 * separado (o limite efetivo vira N × limite); para um limite global, troque o armazenamento por Redis.
 */
@Component
public class RateLimitService {

    public enum Grupo { AUTENTICACAO, API }

    /** Resultado de uma verificação. */
    public record Decisao(boolean permitido, long retryAfterSegundos) {
        static final Decisao PERMITIDO = new Decisao(true, 0);

        static Decisao bloqueado(long segundos) {
            return new Decisao(false, Math.max(1, segundos));
        }
    }

    /** Acima disto, chaves sem requisições recentes são descartadas (evita crescimento sem fim da memória). */
    private static final int LIMITE_CHAVES_PARA_LIMPEZA = 10_000;

    private final boolean habilitado;
    private final int maxAutenticacao;
    private final int maxApi;
    private final long janelaMs;
    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> registros = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitService(
            @Value("${awosbank.seguranca.rate-limit.habilitado:true}") boolean habilitado,
            @Value("${awosbank.seguranca.rate-limit.max-autenticacao:10}") int maxAutenticacao,
            @Value("${awosbank.seguranca.rate-limit.max-api:120}") int maxApi,
            @Value("${awosbank.seguranca.rate-limit.janela-segundos:60}") long janelaSegundos) {
        this(habilitado, maxAutenticacao, maxApi, Duration.ofSeconds(janelaSegundos), Clock.systemUTC());
    }

    /** Construtor para testes (relógio controlável). */
    public RateLimitService(boolean habilitado, int maxAutenticacao, int maxApi, Duration janela, Clock clock) {
        this.habilitado = habilitado;
        this.maxAutenticacao = maxAutenticacao;
        this.maxApi = maxApi;
        this.janelaMs = janela.toMillis();
        this.clock = clock;
    }

    public boolean habilitado() { return habilitado; }

    /** Registra uma requisição do IP no grupo e diz se ela pode seguir. */
    public Decisao verificar(Grupo grupo, String ip) {
        if (!habilitado) return Decisao.PERMITIDO;

        int limite = grupo == Grupo.AUTENTICACAO ? maxAutenticacao : maxApi;
        String chave = grupo + ":" + (ip == null || ip.isBlank() ? "desconhecido" : ip);
        long agora = clock.millis();

        if (registros.size() > LIMITE_CHAVES_PARA_LIMPEZA) limpar(agora);

        Deque<Long> momentos = registros.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (momentos) {
            descartarExpirados(momentos, agora);
            if (momentos.size() >= limite) {
                long esperaMs = janelaMs - (agora - momentos.peekFirst());
                return Decisao.bloqueado((long) Math.ceil(esperaMs / 1000.0));
            }
            momentos.addLast(agora);
            return Decisao.PERMITIDO;
        }
    }

    private void descartarExpirados(Deque<Long> momentos, long agora) {
        while (!momentos.isEmpty() && agora - momentos.peekFirst() >= janelaMs) {
            momentos.pollFirst();
        }
    }

    /**
     * Remove as chaves já vazias. Uma requisição concorrente pode gravar numa fila que acabou de ser
     * removida e perder essa contagem: erro aceitável (o limite fica um pouco mais frouxo, nunca mais duro).
     */
    private void limpar(long agora) {
        registros.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                descartarExpirados(e.getValue(), agora);
                return e.getValue().isEmpty();
            }
        });
    }
}
