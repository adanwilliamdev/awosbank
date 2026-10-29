package com.awosbank.api.dto;

/** Corpo do pedido de encerramento de conta: a senha confirma a identidade do titular. */
public record EncerrarContaRequest(String senha) {
}
