package com.awosbank.api.dto;

import java.math.BigDecimal;

/** Uma notificação do sino: movimentação recente da conta do cliente. */
public record NotificacaoResposta(
        String titulo,
        String descricao,
        String tipo,
        boolean entrada,
        BigDecimal valor,
        String dataHora,
        String comprovante) {
}
