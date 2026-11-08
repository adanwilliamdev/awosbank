package com.awosbank.api.exception;

import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/** Regras compartilhadas pelos tratadores de erros inesperados ({@link GlobalExceptionHandler} e {@link ApiExceptionHandler}). */
final class TratamentoDeErros {

    private TratamentoDeErros() {}

    /**
     * Identificador curto da ocorrência. Vai no log (junto do stack trace) e é mostrado ao usuário,
     * para que quem opera consiga achar o erro certo a partir do que o usuário reportar.
     */
    static String novoCodigo() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * {@code true} para exceções que NÃO são "erro inesperado" e já têm tratamento próprio: o handler
     * genérico deve relançá-las para o Spring responder com o status certo.
     *
     * <ul>
     *   <li>erros HTTP do próprio MVC (400, 404, 405, 415…) e {@code ResponseStatusException};</li>
     *   <li>parâmetro/corpo inválido (400);</li>
     *   <li>exceções anotadas com {@code @ResponseStatus};</li>
     *   <li>acesso negado / não autenticado: quem decide (403 ou redirecionamento ao login) é o Spring Security.</li>
     * </ul>
     *
     * Sem isso, um {@code catch (Exception)} transformaria todos eles em "500 erro inesperado".
     */
    static boolean deveRepassar(Exception ex) {
        return ex instanceof ErrorResponse
                || ex instanceof TypeMismatchException
                || ex instanceof HttpMessageNotReadableException
                || ex instanceof AccessDeniedException
                || ex instanceof AuthenticationException
                || AnnotatedElementUtils.hasAnnotation(ex.getClass(), ResponseStatus.class);
    }
}
