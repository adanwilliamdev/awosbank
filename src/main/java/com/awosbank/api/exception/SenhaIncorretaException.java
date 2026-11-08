package com.awosbank.api.exception;

/**
 * Senha de confirmação errada. É uma {@link BusinessException} (mesma resposta 400 de sempre), mas as
 * operações que a lançam usam {@code noRollbackFor}: a falha é contada no mesmo commit, sem abrir
 * uma segunda transação/conexão por tentativa.
 */
public class SenhaIncorretaException extends BusinessException {
    public SenhaIncorretaException() {
        super("Senha incorreta");
    }
}
