package com.awosbank.api.audit;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.service.AuditoriaService;
import com.awosbank.api.service.ProtecaoForcaBrutaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Audita logins (sucesso e falha) a partir dos eventos publicados pelo Spring Security e alimenta o
 * bloqueio temporário contra força bruta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaAutenticacaoListener {

    private final AuditoriaService auditoriaService;
    private final ProtecaoForcaBrutaService protecao;

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        auditoriaService.registrarPorEmail(AcaoAuditoria.LOGIN_SUCESSO, evento.getAuthentication().getName(), true, null);
        contar(() -> protecao.registrarSucessoLogin(evento.getAuthentication().getName()));
    }

    @EventListener
    public void aoFalhar(AbstractAuthenticationFailureEvent evento) {
        // só o tipo da falha: nunca a senha informada
        auditoriaService.registrarPorEmail(AcaoAuditoria.LOGIN_FALHA, evento.getAuthentication().getName(), false,
                "motivo=" + evento.getException().getClass().getSimpleName());
        // Só senha/usuário inválidos contam (e usuário inexistente chega aqui como BadCredentials, sem diferença
        // visível). Tentativas durante um bloqueio (LockedException) não contam nem estendem o bloqueio.
        if (evento.getException() instanceof BadCredentialsException) {
            contar(() -> protecao.registrarFalhaLogin(evento.getAuthentication().getName()));
        }
    }

    /** Falha ao contar não pode derrubar o login nem esconder o erro original. */
    private void contar(Runnable acao) {
        try {
            acao.run();
        } catch (RuntimeException e) {
            log.error("Falha ao atualizar a proteção contra força bruta: {}", e.toString());
        }
    }
}
