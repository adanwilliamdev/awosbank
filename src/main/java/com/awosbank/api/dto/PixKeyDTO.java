package com.awosbank.api.dto;

import com.awosbank.api.domain.enums.TipoChavePix;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PixKeyDTO {
    @NotNull(message = "Tipo é obrigatório")
    private TipoChavePix tipo;

    // Em branco só é aceito para ALEATORIA (o servidor gera o UUID); o resto é validado no PixKeyService
    @Size(max = 120, message = "Chave muito longa")
    private String chave;
}