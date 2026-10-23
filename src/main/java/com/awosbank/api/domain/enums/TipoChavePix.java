package com.awosbank.api.domain.enums;

public enum TipoChavePix {
    CPF("CPF"),
    EMAIL("E-mail"),
    TELEFONE("Telefone"),
    ALEATORIA("Chave Aleatória");

    private final String descricao;
    TipoChavePix(String d) { this.descricao = d; }
    public String getDescricao() { return descricao; }
}