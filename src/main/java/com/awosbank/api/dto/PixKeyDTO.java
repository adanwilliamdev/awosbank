package com.awosbank.api.dto;

import com.awosbank.api.domain.enums.TipoChavePix;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PixKeyDTO {
    @NotNull(message = "Tipo é obrigatório")
    private TipoChavePix tipo;

    @NotBlank(message = "Chave é obrigatória")
    private String chave;
}