package com.awosbank.api.config;

import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.service.TransacaoService;
import com.awosbank.api.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final TransacaoService transacaoService;

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
            Usuario u = usuarioService.cadastrar(dto);

            // depósito inicial + movimentações de exemplo
            transacaoService.depositar(u, new BigDecimal("2500.00"));
            transacaoService.sacar(u, new BigDecimal("250.00"));
            transacaoService.sacar(u, new BigDecimal("120.00"));

            System.out.println("====================================================");
            System.out.println("  ✓ Conta de teste pronta!");
            System.out.println("  Email: cliente@awosbank.com");
            System.out.println("  Senha: 123456");
            System.out.println("  Saldo inicial: R$ 2.130,00");
            System.out.println("====================================================");
        } catch (Exception e) {
            System.out.println("DataInitializer: " + e.getMessage());
        }
    }
}