package com.awosbank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Dados de um comprovante de transação")
public record ComprovanteResposta(
        boolean ok,
        @Schema(example = "CMP-A1B2C3D4E5F6") String codigo,
        @Schema(example = "PIX") String tipo,
        BigDecimal valor,
        @Schema(description = "ISO-8601", example = "2026-10-24T01:11:00") String dataHora,
        @Schema(description = "Agência / conta de origem (nulo em depósitos)", example = "0001 / 123456-7") String origem,
        @Schema(description = "Agência / conta de destino (nulo em saques)", example = "0001 / 765432-1") String destino) {
}
