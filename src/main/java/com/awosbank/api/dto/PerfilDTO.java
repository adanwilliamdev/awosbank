package com.awosbank.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PerfilDTO {
    @NotBlank
    @Size(min = 3, max = 120)
    private String nomeCompleto;

    @NotBlank @Email
    private String email;

    @Size(max = 20)
    private String telefone;

    private String dataNascimento;

    @Size(max = 200)
    private String endereco;
}