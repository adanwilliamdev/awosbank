package com.awosbank.api;

import com.awosbank.api.domain.enums.TipoConta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.service.UsuarioService;

import java.util.concurrent.atomic.AtomicInteger;

/** Utilitários para criar usuários válidos e únicos nos testes de integração. */
public final class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger(1000);

    private TestData() {}

    /** Gera um CPF válido (com dígitos verificadores corretos) a partir de uma semente. */
    public static String cpfValido(int seed) {
        String base = String.format("%09d", 100_000_000 + seed);
        int d1 = digito(base, 10);
        int d2 = digito(base + d1, 11);
        return base + d1 + d2;
    }

    private static int digito(String s, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < s.length(); i++) soma += (s.charAt(i) - '0') * (pesoInicial - i);
        int r = (soma * 10) % 11;
        return r == 10 ? 0 : r;
    }

    public static Usuario novoUsuario(UsuarioService service, String senha) {
        int n = SEQ.incrementAndGet();
        CadastroDTO dto = new CadastroDTO();
        dto.setNomeCompleto("Usuario Teste " + n);
        dto.setCpfCnpj(cpfValido(n));
        dto.setEmail("teste" + n + "@awosbank.test");
        dto.setSenha(senha);
        dto.setConfirmarSenha(senha);
        dto.setTipoConta(TipoConta.CORRENTE);
        return service.cadastrar(dto);
    }
}
