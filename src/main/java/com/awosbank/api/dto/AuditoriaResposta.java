package com.awosbank.api.dto;

import com.awosbank.api.domain.model.Auditoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Registro do histórico de auditoria")
public record AuditoriaResposta(
        UUID id,
        LocalDateTime dataHora,
        @Schema(example = "TRANSFERENCIA") String acao,
        @Schema(example = "Transferência") String descricao,
        boolean sucesso,
        @Schema(example = "tipo=PIX; valor=10.00; destino=123456-7; comprovante=CMP-A1B2C3D4E5F6") String detalhes,
        @Schema(example = "203.0.113.10") String ipOrigem) {

    public static AuditoriaResposta de(Auditoria a) {
        return new AuditoriaResposta(a.getId(), a.getDataHora(), a.getAcao().name(),
                a.getAcao().getDescricao(), a.isSucesso(), a.getDetalhes(), a.getIpOrigem());
    }
}
