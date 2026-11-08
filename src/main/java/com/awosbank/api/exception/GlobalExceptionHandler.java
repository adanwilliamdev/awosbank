package com.awosbank.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Erros das páginas web (Thymeleaf). Os controllers REST ({@code controller.api}) têm o
 * {@link ApiExceptionHandler}, que tem precedência e responde em JSON.
 */
@Slf4j
@ControllerAdvice(basePackages = "com.awosbank.api.controller")
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    static final String MENSAGEM_GENERICA = "Não foi possível concluir a operação. Tente novamente em instantes.";

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        model.addAttribute("titulo", "Operação não permitida");
        model.addAttribute("mensagem", ex.getMessage());
        return "error";
    }

    /**
     * Erro inesperado: o stack trace completo vai para o LOG (com um código de ocorrência) e o usuário
     * recebe só uma mensagem genérica — {@code ex.getMessage()} pode conter SQL, URLs de banco ou detalhes
     * de framework que não devem sair do servidor.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, HttpServletRequest request, Model model) throws Exception {
        if (TratamentoDeErros.deveRepassar(ex)) {
            throw ex; // 400/404/405, acesso negado etc.: o Spring responde com o status adequado
        }
        String codigo = TratamentoDeErros.novoCodigo();
        log.error("Erro inesperado [codigo={}] em {} {}", codigo, request.getMethod(), request.getRequestURI(), ex);
        model.addAttribute("titulo", "Erro inesperado");
        model.addAttribute("mensagem", MENSAGEM_GENERICA);
        model.addAttribute("codigoErro", codigo);
        return "error";
    }
}
