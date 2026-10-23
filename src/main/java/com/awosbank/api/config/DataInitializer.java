package com.awosbank.api.config;

import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        try {
            CadastroDTO dto = new CadastroDTO();
            dto.setNomeCompleto("Cliente de Teste");
            dto.setCpfCnpj("11144477735");
            dto.setEmail("cliente@awosbank.com");
            dto.setSenha("123456");
            dto.setConfirmarSenha("123456");
            dto.setTipoConta(TipoConta.CORRENTE);
            usuarioService.cadastrar(dto);
            System.out.println("====================================================");
            System.out.println("  Usuário de teste criado:");
            System.out.println("  Email: cliente@awosbank.com");
            System.out.println("  Senha: 123456");
            System.out.println("====================================================");
        } catch (Exception e) {
            System.out.println("DataInitializer: " + e.getMessage());
        }
    }
}