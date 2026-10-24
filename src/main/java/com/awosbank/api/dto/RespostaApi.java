package com.awosbank.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Envelope padrão das respostas da API REST (campos nulos não são serializados). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado de uma operação da API")
public record RespostaApi(
        @Schema(description = "true se a operação foi concluída", example = "true") boolean ok,
        @Schema(description = "Mensagem para exibição ao usuário", example = "Transferência realizada com sucesso!") String mensagem,
        @Schema(description = "Código do comprovante, quando a operação gera um", example = "CMP-A1B2C3D4E5F6") String comprovante,
        @Schema(description = "Identificador do recurso criado, quando houver") UUID id) {

    public static RespostaApi sucesso(String mensagem) {
        return new RespostaApi(true, mensagem, null, null);
    }

    public static RespostaApi comprovante(String mensagem, String codigo) {
        return new RespostaApi(true, mensagem, codigo, null);
    }

    public static RespostaApi criado(String mensagem, UUID id) {
        return new RespostaApi(true, mensagem, null, id);
    }

    public static RespostaApi erro(String mensagem) {
        return new RespostaApi(false, mensagem, null, null);
    }
}
