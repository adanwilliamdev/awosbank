package com.awosbank.api.dto;

import com.awosbank.api.domain.enums.TipoConta;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CadastroDTO {

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 120)
    private String nomeCompleto;

    @NotBlank(message = "CPF/CNPJ é obrigatório")
    private String cpfCnpj;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 60, message = "Senha deve ter entre 6 e 60 caracteres")
    private String senha;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmarSenha;

    @NotNull(message = "Tipo de conta é obrigatório")
    private TipoConta tipoConta;
}