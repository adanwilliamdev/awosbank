package com.awosbank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Corpo das requisições de depósito e saque. */
@Schema(description = "Valor da operação")
public record ValorRequest(
        @Schema(description = "Valor em reais, até 2 casas decimais (máx. R$ 1.000.000,00)", example = "150.75")
        BigDecimal valor) {
}
