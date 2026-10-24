package com.awosbank.api.audit;

import com.awosbank.api.domain.enums.AcaoAuditoria;
import com.awosbank.api.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Audita logins (sucesso e falha) a partir dos eventos publicados pelo Spring Security. */
@Component
@RequiredArgsConstructor
public class AuditoriaAutenticacaoListener {

    private final AuditoriaService auditoriaService;

    @EventListener
    public void aoAutenticar(AuthenticationSuccessEvent evento) {
        auditoriaService.registrar(AcaoAuditoria.LOGIN_SUCESSO, evento.getAuthentication().getName(), true, null);
    }

    @EventListener
    public void aoFalhar(AbstractAuthenticationFailureEvent evento) {
        // só o tipo da falha: nunca a senha informada
        auditoriaService.registrar(AcaoAuditoria.LOGIN_FALHA, evento.getAuthentication().getName(), false,
                "motivo=" + evento.getException().getClass().getSimpleName());
    }
}
