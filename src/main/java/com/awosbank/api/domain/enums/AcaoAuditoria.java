package com.awosbank.api.domain.enums;

/** Operações que entram no histórico de auditoria. */
public enum AcaoAuditoria {
    CADASTRO("Cadastro de cliente"),
    LOGIN_SUCESSO("Login realizado"),
    LOGIN_FALHA("Falha de login"),
    ATUALIZACAO_PERFIL("Atualização de perfil"),
    TRANSFERENCIA("Transferência"),
    DEPOSITO("Depósito"),
    SAQUE("Saque"),
    PIX_CHAVE_CADASTRADA("Chave PIX cadastrada"),
    PIX_CHAVE_REMOVIDA("Chave PIX removida"),
    CONTA_ENCERRADA("Conta encerrada");

    private final String descricao;

    AcaoAuditoria(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}
