package com.awosbank.api.domain.enums;

public enum StatusConta {
    ATIVA("Ativa"),
    BLOQUEADA("Bloqueada"),
    ENCERRADA("Encerrada");

    private final String descricao;

    StatusConta(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}