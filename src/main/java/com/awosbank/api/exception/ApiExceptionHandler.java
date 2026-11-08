package com.awosbank.api.exception;

import com.awosbank.api.dto.RespostaApi;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros da API REST (/api/**) em JSON. As páginas Thymeleaf continuam com o {@link GlobalExceptionHandler}. */
@Slf4j
@RestControllerAdvice(basePackages = "com.awosbank.api.controller.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RespostaApi> regraDeNegocio(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(RespostaApi.erro(ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespostaApi> corpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(RespostaApi.erro("Corpo da requisição inválido"));
    }

    /**
     * Erro inesperado na API: JSON genérico com um código de ocorrência (500); o stack trace completo
     * fica só no log. Sem este handler, a exceção cairia no {@link GlobalExceptionHandler} e o cliente
     * da API receberia a página HTML de erro.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaApi> erroInesperado(Exception ex, HttpServletRequest request) throws Exception {
        if (TratamentoDeErros.deveRepassar(ex)) {
            throw ex; // 400/404/405, acesso negado etc.: o Spring responde com o status adequado
        }
        String codigo = TratamentoDeErros.novoCodigo();
        log.error("Erro inesperado na API [codigo={}] em {} {}", codigo, request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RespostaApi.erro(
                "Erro interno ao processar a requisição. Se o problema persistir, informe o código " + codigo + "."));
    }
}
