package com.awosbank.api.dto;

import com.awosbank.api.domain.enums.TipoTransacao;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferenciaDTO {

    @NotBlank(message = "Conta destino é obrigatória")
    private String numeroContaDestino;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @DecimalMax(value = "1000000.00", message = "Valor máximo é R$ 1.000.000,00")
    private BigDecimal valor;

    @NotNull(message = "Tipo é obrigatório")
    private TipoTransacao tipo = TipoTransacao.PIX;

    @NotBlank(message = "Senha é obrigatória para confirmar")
    private String senha;
}