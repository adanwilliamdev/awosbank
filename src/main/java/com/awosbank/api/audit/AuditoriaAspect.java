package com.awosbank.api.audit;

import com.awosbank.api.domain.model.Transacao;
import com.awosbank.api.domain.model.PixKey;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.dto.TransferenciaDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registra no histórico de auditoria toda chamada a um método {@link Auditado}.
 *
 * <p>O aspecto roda <b>por fora</b> da transação do serviço (ordem de precedência mais alta que a do
 * {@code @Transactional}). Assim o registro é gravado numa transação própria, e as tentativas que
 * falham (e fazem rollback da operação) também ficam registradas.
 *
 * <p>Os detalhes são montados somente a partir de campos conhecidos e não sensíveis — nunca com
 * {@code toString()} dos DTOs, que carregam senha.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    @Around("@annotation(auditado)")
    public Object auditar(ProceedingJoinPoint pjp, Auditado auditado) throws Throwable {
        Object[] args = pjp.getArgs();
        try {
            Object resultado = pjp.proceed();
            auditoriaService.registrar(auditado.value(), ator(args), true, detalhes(args, resultado, null));
            return resultado;
        } catch (Throwable erro) {
            auditoriaService.registrar(auditado.value(), ator(args), false, detalhes(args, null, erro));
            throw erro;
        }
    }

    /** Quem executou: usuário autenticado; senão, o e-mail informado no argumento (ex.: cadastro). */
    static String ator(Object[] args) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getName() != null) {
            return auth.getName();
        }
        for (Object arg : args) {
            if (arg instanceof Usuario u && u.getEmail() != null) return u.getEmail();
            if (arg instanceof CadastroDTO c && c.getEmail() != null) return c.getEmail();
        }
        return "anonimo";
    }

    static String detalhes(Object[] args, Object resultado, Throwable erro) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof TransferenciaDTO d) {
                adicionar(sb, "tipo", d.getTipo());
                adicionar(sb, "valor", d.getValor());
                adicionar(sb, "destino", d.getNumeroContaDestino());
            } else if (arg instanceof BigDecimal valor) {
                adicionar(sb, "valor", valor);
            } else if (arg instanceof UUID id) {
                adicionar(sb, "id", id);
            }
        }
        if (resultado instanceof Transacao t) {
            adicionar(sb, "comprovante", t.getComprovanteCodigo());
        } else if (resultado instanceof PixKey k) {
            adicionar(sb, "pixKeyId", k.getId());
            adicionar(sb, "tipoChave", k.getTipo());
        } else if (resultado instanceof Usuario u) {
            adicionar(sb, "usuarioId", u.getId());
        }
        if (erro != null) {
            String motivo = erro instanceof BusinessException
                    ? erro.getMessage()
                    : "erro inesperado (" + erro.getClass().getSimpleName() + ")";
            adicionar(sb, "motivo", motivo);
        }
        return sb.toString();
    }

    private static void adicionar(StringBuilder sb, String chave, Object valor) {
        if (valor == null) return;
        if (sb.length() > 0) sb.append("; ");
        sb.append(chave).append('=').append(valor);
    }
}
