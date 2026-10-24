package com.awosbank.api.audit;

import com.awosbank.api.domain.enums.AcaoAuditoria;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de serviço cuja execução (com sucesso OU com falha) deve ir para o histórico
 * de auditoria. Tratado por {@link AuditoriaAspect}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditado {
    AcaoAuditoria value();
}
