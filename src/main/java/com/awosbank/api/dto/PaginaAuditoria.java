package com.awosbank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Página do histórico de auditoria")
public record PaginaAuditoria(
        List<AuditoriaResposta> itens,
        @Schema(description = "Página atual (começa em 0)") int pagina,
        int tamanho,
        long total,
        int totalPaginas) {

    public static PaginaAuditoria de(Page<?> page, List<AuditoriaResposta> itens) {
        return new PaginaAuditoria(itens, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
