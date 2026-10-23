package com.awosbank.api.dto;

import com.awosbank.api.domain.model.Transacao;

public record TransacaoView(Transacao transacao, boolean entrada, String descricao) {
}