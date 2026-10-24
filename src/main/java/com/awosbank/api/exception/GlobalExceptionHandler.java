package com.awosbank.api.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Erros das páginas web (Thymeleaf). Não se aplica ao springdoc nem a endpoints fora do pacote de controllers. */
@ControllerAdvice(basePackages = "com.awosbank.api.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        model.addAttribute("titulo", "Operação não permitida");
        model.addAttribute("mensagem", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        model.addAttribute("titulo", "Erro inesperado");
        model.addAttribute("mensagem", "Ocorreu um erro: " + ex.getMessage());
        return "error";
    }
}