package com.awosbank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Chave PIX cadastrada")
public record ChavePixResposta(
        UUID id,
        @Schema(example = "EMAIL") String tipo,
        @Schema(example = "E-mail") String tipoDescricao,
        @Schema(example = "cliente@awosbank.com") String chave,
        @Schema(description = "ISO-8601", example = "2026-10-24T01:11:00") String dataCriacao) {
}
