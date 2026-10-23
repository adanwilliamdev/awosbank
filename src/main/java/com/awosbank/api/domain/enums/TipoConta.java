package com.awosbank.api.domain.enums;

public enum TipoConta {
    CORRENTE("Conta Corrente"),
    POUPANCA("Conta Poupança"),
    EMPRESARIAL("Conta Empresarial");

    private final String descricao;

    TipoConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}