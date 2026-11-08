package com.awosbank.api.service;

import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bloqueio temporário contra tentativa de adivinhar senha.
 *
 * <p>Dois escopos independentes por usuário: <b>login</b> e <b>senha das operações</b> (confirmação de
 * transferência e de encerramento de conta — o que uma sessão roubada tentaria). Após
 * {@code max-tentativas} falhas dentro de {@code janela-minutos}, o escopo fica bloqueado por
 * {@code bloqueio-minutos}; enquanto bloqueado, nem a senha correta é aceita. O contador zera quando a
 * senha é digitada corretamente. Os limites ficam em {@code awosbank.seguranca.*} (application.yml).
 *
 * <p>Os contadores são atualizados com UPDATE atômico no banco: seguro com várias instâncias e sob
 * requisições paralelas. Os métodos participam da transação do chamador; no login (sem transação) cada
 * um abre a sua.
 */
@Slf4j
@Service
public class ProtecaoForcaBrutaService {

    private final UsuarioRepository usuarioRepository;
    private final int maxTentativas;
    private final Duration janela;
    private final Duration bloqueio;

    public ProtecaoForcaBrutaService(UsuarioRepository usuarioRepository,
                                     @Value("${awosbank.seguranca.max-tentativas:5}") int maxTentativas,
                                     @Value("${awosbank.seguranca.janela-minutos:15}") int janelaMinutos,
                                     @Value("${awosbank.seguranca.bloqueio-minutos:15}") int bloqueioMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.maxTentativas = maxTentativas;
        this.janela = Duration.ofMinutes(janelaMinutos);
        this.bloqueio = Duration.ofMinutes(bloqueioMinutos);
    }

    // ---------------------------------------------------------------- login

    /** Login bloqueado agora? (usado para marcar a conta como "locked" no Spring Security). */
    @Transactional(readOnly = true)
    public boolean loginBloqueado(String email) {
        return vigente(usuarioRepository.bloqueadoAteLogin(normalizar(email)));
    }

    @Transactional
    public void registrarFalhaLogin(String email) {
        String e = normalizar(email);
        LocalDateTime agora = LocalDateTime.now();
        usuarioRepository.reiniciarJanelaLogin(e, agora, agora.minus(janela));
        usuarioRepository.incrementarFalhaLogin(e, agora);
        if (usuarioRepository.bloquearLogin(e, maxTentativas, agora, agora.plus(bloqueio)) > 0) {
            log.warn("Bloqueio temporário de LOGIN aplicado por {} minuto(s) após {} falhas", bloqueio.toMinutes(), maxTentativas);
        }
    }

    @Transactional
    public void registrarSucessoLogin(String email) {
        usuarioRepository.limparFalhasLogin(normalizar(email), LocalDateTime.now());
    }

    // ------------------------------------------------- senha das operações

    /** Lança {@link BusinessException} se as confirmações por senha deste usuário estão bloqueadas. */
    @Transactional(readOnly = true)
    public void exigirOperacaoLiberada(String email) {
        Optional<LocalDateTime> ate = usuarioRepository.bloqueadoAteSenhaOp(normalizar(email));
        if (vigente(ate)) {
            long minutos = Math.max(1, (Duration.between(LocalDateTime.now(), ate.get()).getSeconds() + 59) / 60);
            throw new BusinessException("Muitas tentativas de senha incorretas. Tente novamente em "
                    + minutos + (minutos == 1 ? " minuto." : " minutos."));
        }
    }

    @Transactional
    public void registrarFalhaSenhaOp(String email) {
        String e = normalizar(email);
        LocalDateTime agora = LocalDateTime.now();
        usuarioRepository.reiniciarJanelaSenhaOp(e, agora, agora.minus(janela));
        usuarioRepository.incrementarFalhaSenhaOp(e, agora);
        if (usuarioRepository.bloquearSenhaOp(e, maxTentativas, agora, agora.plus(bloqueio)) > 0) {
            log.warn("Bloqueio temporário de SENHA DE OPERAÇÃO aplicado por {} minuto(s) após {} falhas", bloqueio.toMinutes(), maxTentativas);
        }
    }

    @Transactional
    public void registrarSucessoSenhaOp(String email) {
        usuarioRepository.limparFalhasSenhaOp(normalizar(email), LocalDateTime.now());
    }

    // ------------------------------------------------------------ internos

    private static boolean vigente(Optional<LocalDateTime> ate) {
        return ate.isPresent() && ate.get().isAfter(LocalDateTime.now());
    }

    private static String normalizar(String email) {
        return email == null ? "" : UsuarioService.normalizarEmail(email);
    }
}
