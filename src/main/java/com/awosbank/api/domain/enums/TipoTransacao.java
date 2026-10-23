package com.awosbank.api.domain.enums;

public enum TipoTransacao {
    PIX("PIX"),
    TED("TED"),
    DEPOSITO("Depósito"),
    SAQUE("Saque");

    private final String descricao;

    TipoTransacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}