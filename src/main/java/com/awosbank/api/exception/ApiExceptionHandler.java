package com.awosbank.api.exception;

import com.awosbank.api.dto.RespostaApi;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros da API REST (/api/**) em JSON. As páginas Thymeleaf continuam com o {@link GlobalExceptionHandler}. */
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
}
