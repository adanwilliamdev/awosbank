package com.awosbank.api.service;

import com.awosbank.api.domain.enums.StatusConta;
import com.awosbank.api.domain.model.Conta;
import com.awosbank.api.domain.model.Usuario;
import com.awosbank.api.domain.repository.UsuarioRepository;
import com.awosbank.api.dto.CadastroDTO;
import com.awosbank.api.exception.BusinessException;
import com.awosbank.api.util.DocumentoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ContaService contaService;
    private final PasswordEncoder passwordEncoder;

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));
    }

    @Transactional
    public Usuario cadastrar(CadastroDTO dto) {
        if (!dto.getSenha().equals(dto.getConfirmarSenha())) {
            throw new BusinessException("Senhas não conferem");
        }
        if (!DocumentoValidator.isValid(dto.getCpfCnpj())) {
            throw new BusinessException("CPF/CNPJ inválido");
        }
        String documentoLimpo = dto.getCpfCnpj().replaceAll("\\D", "");
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email já cadastrado");
        }
        if (usuarioRepository.existsByCpfCnpj(documentoLimpo)) {
            throw new BusinessException("CPF/CNPJ já cadastrado");
        }

        Conta conta = contaService.criarConta(dto.getTipoConta());
        conta.setStatus(StatusConta.ATIVA);

        Usuario usuario = Usuario.builder()
                .nomeCompleto(dto.getNomeCompleto())
                .cpfCnpj(documentoLimpo)
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .roles(Set.of("ROLE_CLIENTE"))
                .conta(conta)
                .build();

        return usuarioRepository.save(usuario);
    }
}